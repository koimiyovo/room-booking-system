import { Navigate, Outlet } from 'react-router-dom'
import { useAuthContext } from '@/ui/context/AuthContext'

export default function AdminRoute() {
  const { isAdmin } = useAuthContext()
  return isAdmin ? <Outlet /> : <Navigate to="/rooms" replace />
}
