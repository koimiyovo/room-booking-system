import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { BookingService } from '@/domain/service/BookingService'
import { BookingApiAdapter } from '@/infrastructure/api/BookingApiAdapter'
import type { NewBooking } from '@/domain/model/Booking'

const bookingService = new BookingService(new BookingApiAdapter())

export function useBookings() {
  return useQuery({
    queryKey: ['bookings'],
    queryFn: () => bookingService.findAll(),
  })
}

export function useUserBookings(userId: string) {
  return useQuery({
    queryKey: ['bookings', 'user', userId],
    queryFn: () => bookingService.findByUser(userId),
    enabled: !!userId,
  })
}

export function useCreateBooking() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (booking: NewBooking) => bookingService.create(booking),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['bookings'] })
    },
  })
}

export function useCancelBooking() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, userId, reason }: { id: string; userId: string; reason?: string }) =>
      bookingService.cancel(id, userId, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['bookings'] })
    },
  })
}
