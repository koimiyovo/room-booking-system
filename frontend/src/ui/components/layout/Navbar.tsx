import { Link, useNavigate } from 'react-router-dom'
import { useAuthContext } from '@/ui/context/AuthContext'
import { Button } from '@/components/ui/button'

export default function Navbar() {
  const { user, isAdmin, logout } = useAuthContext()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <header className="border-b bg-white sticky top-0 z-10">
      <div className="container mx-auto px-4 max-w-6xl h-14 flex items-center justify-between">
        <nav className="flex items-center gap-6">
          <Link to="/rooms" className="font-semibold text-sm hover:opacity-70 transition-opacity">
            Room Booking
          </Link>
          <Link to="/rooms" className="text-sm text-gray-500 hover:text-gray-900">
            Rooms
          </Link>
          <Link to="/bookings" className="text-sm text-gray-500 hover:text-gray-900">
            Bookings
          </Link>
          {isAdmin && (
            <Link to="/users" className="text-sm text-gray-500 hover:text-gray-900">
              Users
            </Link>
          )}
        </nav>
        <div className="flex items-center gap-3">
          <span className="text-sm text-gray-500">{user?.email}</span>
          <Button variant="outline" size="sm" onClick={handleLogout}>
            Logout
          </Button>
        </div>
      </div>
    </header>
  )
}
