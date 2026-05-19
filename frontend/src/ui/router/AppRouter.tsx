import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import PrivateRoute from '@/ui/components/layout/PrivateRoute'
import AdminRoute from '@/ui/components/layout/AdminRoute'
import AppLayout from '@/ui/components/layout/AppLayout'
import LoginPage from '@/ui/pages/LoginPage'
import RegisterPage from '@/ui/pages/RegisterPage'
import RoomsPage from '@/ui/pages/RoomsPage'
import BookingsPage from '@/ui/pages/BookingsPage'
import UsersPage from '@/ui/pages/UsersPage'

export default function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        <Route element={<PrivateRoute />}>
          <Route element={<AppLayout />}>
            <Route index element={<Navigate to="/rooms" replace />} />
            <Route path="/rooms" element={<RoomsPage />} />
            <Route path="/bookings" element={<BookingsPage />} />
            <Route element={<AdminRoute />}>
              <Route path="/users" element={<UsersPage />} />
            </Route>
          </Route>
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
