package cn.jongwong.service.impl;

import cn.jongwong.common.ConvertUtils;
import cn.jongwong.config.StorageProperties;
import cn.jongwong.entity.PencilDocument;
import cn.jongwong.repository.PencilFileRepository;
import cn.jongwong.ro.PencilDocumentRequest;
import cn.jongwong.ro.PencilDocumentResponse;
import cn.jongwong.security.SecurityUtils;
import cn.jongwong.service.OssService;
import cn.jongwong.service.PencilDocumentService;
import cn.jongwong.storage.StorageObjectPaths;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;

@Service
@Slf4j
public class PencilDocumentServiceImpl implements PencilDocumentService {

    private static final int KEY_BYTES = 12;
    private static final int MAX_RETRY = 8;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Autowired
    private OssService ossService;

    @Autowired
    private PencilFileRepository pencilFileRepository;

    @Autowired
    private StorageProperties storageProperties;

    @Autowired
    private SecurityUtils securityUtils;

    @Override
    @Transactional
    public PencilDocumentResponse create(PencilDocumentRequest request) {
        String key = generateUniqueKey();
        String directory = StorageObjectPaths.directory("fig", securityUtils.getCurrentUsername());
        String storedPath = ossService.upload(directory, key + ".fig", loadBlankFig());

        long now = System.currentTimeMillis();
        PencilDocument file = PencilDocument.builder()
                .key(key)
                .name(request.getName())
                .description(request.getDescription())
                .teamId(request.getTeamId())
                .projectId(request.getProjectId())
                .url(storedPath)
                .isDeleted(0)
                .createdAt(now)
                .version("1.0.0")
                .updatedAt(now)
                .build();

        PencilDocument saved = pencilFileRepository.save(file);
        log.info("文件创建成功: key={}, url={}", saved.getKey(), saved.getUrl());
        return ConvertUtils.convert(saved, PencilDocumentResponse.class);
    }

    @Override
    @Transactional
    public Boolean updateThumbnail(String key, MultipartFile file) {
        PencilDocument doc = pencilFileRepository.findByKeyAndIsDeleted(key, 0)
                .orElseThrow(() -> new RuntimeException("文件不存在: " + key));

        String directory;
        String fileName;
        String existing = doc.getThumbnailUrl();
        if (isStoredObjectKey(existing)) {
            int slash = existing.lastIndexOf('/');
            if (slash > 0) {
                directory = existing.substring(0, slash).replaceAll("^/+", "");
                fileName = existing.substring(slash + 1);
            } else {
                directory = "";
                fileName = existing.replaceAll("^/+", "");
            }
        } else {
            directory = StorageObjectPaths.directory("img", securityUtils.getCurrentUsername());
            fileName = key + thumbnailExtension(file.getOriginalFilename());
        }
        try {
            String storedPath = ossService.upload(directory, fileName, file.getBytes());
            if (!storedPath.equals(existing)) {
                doc.setThumbnailUrl(storedPath);
            }
            doc.setUpdatedAt(System.currentTimeMillis());
            pencilFileRepository.save(doc);
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PencilDocumentResponse getByKey(String key) {
        log.debug("根据 KEY 查询文件: {}", key);
        PencilDocument file = pencilFileRepository.findByKeyAndIsDeleted(key, 0)
                .orElseThrow(() -> new RuntimeException("文件不存在: " + key));
        return ConvertUtils.convert(file, PencilDocumentResponse.class);
    }

    @Override
    public List<PencilDocumentResponse> getAllFiles() {
        log.debug("获取所有文件列表");
        List<PencilDocument> files = pencilFileRepository.findByIsDeletedOrderByUpdatedAtDesc(0);
        return ConvertUtils.convertList(files, PencilDocumentResponse.class);
    }

    @Override
    @Transactional
    public PencilDocumentResponse update(String key, PencilDocumentRequest request) {
        log.info("更新文件: key={}", key);
        PencilDocument existing = pencilFileRepository.findByKeyAndIsDeleted(key, 0)
                .orElseThrow(() -> new RuntimeException("文件不存在: " + key));

        if (request.getName() != null) {
            existing.setName(request.getName());
        }
        if (request.getDescription() != null) {
            existing.setDescription(request.getDescription());
        }
        if (request.getTeamId() != null) {
            existing.setTeamId(request.getTeamId());
        }
        if (request.getProjectId() != null) {
            existing.setProjectId(request.getProjectId());
        }
        if (request.getVersion() != null) {
            existing.setVersion(request.getVersion());
        }

        existing.setUpdatedAt(System.currentTimeMillis());
        PencilDocument updated = pencilFileRepository.save(existing);
        log.info("文件更新成功: key={}", key);
        return ConvertUtils.convert(updated, PencilDocumentResponse.class);
    }

    @Override
    @Transactional
    public void delete(String key) {
        log.info("删除文件: key={}", key);
        PencilDocument existing = pencilFileRepository.findByKeyAndIsDeleted(key, 0)
                .orElseThrow(() -> new RuntimeException("文件不存在: " + key));
        existing.setIsDeleted(1);
        existing.setUpdatedAt(System.currentTimeMillis());
        pencilFileRepository.save(existing);
        log.info("文件已删除: key={}", key);
    }

    private String generateUniqueKey() {
        for (int i = 0; i < MAX_RETRY; i++) {
            String key = generateRandomKey();
            if (!pencilFileRepository.existsByKeyAndIsDeleted(key, 0)) {
                return key;
            }
            log.warn("KEY 冲突: {}, 重试第 {} 次", key, i + 1);
        }
        throw new RuntimeException("Failed to generate a unique document key");
    }

    private String generateRandomKey() {
        byte[] bytes = new byte[KEY_BYTES];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private byte[] loadBlankFig() {
        String url = storageProperties.getBlankFigUrl();
        if (url == null || url.isBlank()) {
            throw new RuntimeException("storage.blank-fig-url is not configured");
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(30))
                    .build();
            HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("Failed to download blank.fig: HTTP " + response.statusCode());
            }
            byte[] body = response.body();
            if (body == null || body.length == 0) {
                throw new RuntimeException("blank.fig is empty");
            }
            return body;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while downloading blank.fig", e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to download blank.fig", e);
        }
    }

    private static boolean isStoredObjectKey(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String value = url.trim();
        return !value.startsWith("/") && !value.startsWith("http://") && !value.startsWith("https://");
    }

    private static String thumbnailExtension(String originalName) {
        if (originalName == null) {
            return ".png";
        }
        String name = originalName.trim();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return ".png";
        }
        String ext = name.substring(dot).toLowerCase();
        if (ext.matches("\\.(png|jpe?g|webp|gif|avif)")) {
            return ext.equals(".jpeg") ? ".jpg" : ext;
        }
        return ".png";
    }
}
