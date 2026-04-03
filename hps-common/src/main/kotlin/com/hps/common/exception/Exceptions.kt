package com.hps.common.exception

open class HpsException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class NotFoundException(entity: String, id: Any) : HpsException("$entity not found: $id")

class BadRequestException(message: String) : HpsException(message)

class UnauthorizedException(message: String = "Unauthorized") : HpsException(message)

class ForbiddenException(message: String = "Forbidden") : HpsException(message)
