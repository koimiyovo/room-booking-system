package com.kyovo.domain.exception

import com.kyovo.domain.model.BookingNumberOfPeople
import com.kyovo.domain.model.RoomCapacity

class RoomCapacityExceededException(
    requested: BookingNumberOfPeople,
    val capacity: RoomCapacity
) : RuntimeException("Room capacity exceeded: requested ${requested.value}, capacity is ${capacity.value}")
