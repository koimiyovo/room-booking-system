import { Navigate, Outlet } from 'react-router-dom'
import { useAuthContext } from '@/ui/context/AuthContext'

export default function PrivateRoute() {
  const { isAuthenticated } = useAuthContext()
  return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace />
}
