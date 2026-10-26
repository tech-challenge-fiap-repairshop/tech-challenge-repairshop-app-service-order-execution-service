package com.cao.repairshop.core.validator.impl

import com.cao.repairshop.core.validator.annotation.LicensePlate
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class LicensePlateValidator : ConstraintValidator<LicensePlate, String> {

    companion object {
        private val PLATE_REGEX = Regex("^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$", RegexOption.IGNORE_CASE)
    }

    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value.isNullOrBlank()) return false
        val sanitized = value.replace("-", "").uppercase()
        return PLATE_REGEX.matches(sanitized)
    }
}
