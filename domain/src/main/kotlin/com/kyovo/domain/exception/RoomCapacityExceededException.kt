package com.kyovo.domain.exception

import com.kyovo.domain.model.booking.BookingNumberOfPeople
import com.kyovo.domain.model.room.RoomCapacity

class RoomCapacityExceededException(
    requested: BookingNumberOfPeople,
    val capacity: RoomCapacity
) : RuntimeException("Room capacity exceeded: requested ${requested.value}, capacity is ${capacity.value}")
