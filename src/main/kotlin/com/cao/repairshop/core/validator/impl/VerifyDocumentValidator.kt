package com.cao.repairshop.core.validator.impl

import com.cao.repairshop.core.validator.annotation.VerifyDocument
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class VerifyDocumentValidator : ConstraintValidator<VerifyDocument, String> {

    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value.isNullOrBlank()) return true
        val clean = value.filter { it.isDigit() }
        if (clean.length != 11 || clean.all { it == clean[0] }) return false

        val d1 = ((0..8).sumOf { (clean[it] - '0') * (10 - it) } * 10 % 11).let { if (it == 10) 0 else it }
        if (clean[9] - '0' != d1) return false

        val d2 = ((0..9).sumOf { (clean[it] - '0') * (11 - it) } * 10 % 11).let { if (it == 10) 0 else it }
        return clean[10] - '0' == d2
    }
}
