import { consumeReturnTo } from '#react/app/auth/redirect'
import { useI18n } from '#react/i18n'
import { authAPI, getAPIErrorMessage } from '#react/lib/client'
import { AuthShell } from '#react/view/auth/AuthShell'
import { LoaderCircle } from 'lucide-react'
import { useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'

export default function OauthCallbackView() {
  const { auth } = useI18n()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  useEffect(() => {
    const ticket = searchParams.get('ticket')
    if (!ticket) {
      void navigate('/login', { replace: true })
      return
    }
    let cancelled = false
    void authAPI
      .oauthSession(ticket)
      .then(() => {
        if (cancelled) return
        void navigate(consumeReturnTo(), { replace: true })
      })
      .catch((cause: unknown) => {
        if (cancelled) return
        const message = getAPIErrorMessage(cause, 'Sign in failed')
        void navigate(`/login?error=${encodeURIComponent(message)}`, { replace: true })
      })
    return () => {
      cancelled = true
    }
  }, [navigate, searchParams])

  return (
    <div className="h-full" data-test-id="oauth-callback-page">
      <AuthShell title={auth.appName || 'Welcome Back'} subtitle={auth.oauthWait || 'Finishing sign in…'}>
        <p className="flex items-center justify-center gap-2 text-sm text-muted">
          <LoaderCircle className="size-4 animate-spin" />
          {auth.oauthWait || 'Finishing sign in…'}
        </p>
      </AuthShell>
    </div>
  )
}
