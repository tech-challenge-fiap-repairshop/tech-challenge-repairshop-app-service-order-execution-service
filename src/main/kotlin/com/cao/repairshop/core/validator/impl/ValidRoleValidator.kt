package com.cao.repairshop.core.validator.impl

import com.cao.repairshop.core.security.UserRole
import com.cao.repairshop.core.validator.annotation.ValidRole
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class ValidRoleValidator : ConstraintValidator<ValidRole, String> {

    private val validValues = UserRole.entries.map { it.name }.toSet()

    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value.isNullOrBlank()) return true
        return value.uppercase() in validValues
    }
}
