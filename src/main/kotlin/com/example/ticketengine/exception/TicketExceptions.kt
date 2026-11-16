package com.example.ticketengine.exception

class ShowNotFoundException(message: String) : RuntimeException(message)
class SeatNotFoundException(message: String) : RuntimeException(message)
class SeatNotAvailableException(message: String) : RuntimeException(message)
class UnauthorizedHoldException(message: String) : RuntimeException(message)