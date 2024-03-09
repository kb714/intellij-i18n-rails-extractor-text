package com.kb714.i18nrailstextextractor.utils

import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.Messages

object UserInteraction {

    fun askForInput() : String?{
        val userInput = Messages.showInputDialog(
            "Ingresa la clave del texto a Extraer",
            "Información",
            Messages.getQuestionIcon(),
            "",
            object : InputValidator {
                override fun checkInput(inputString: String?): Boolean {
                    return !inputString.isNullOrEmpty()
                }

                override fun canClose(inputString: String?): Boolean {
                    return checkInput(inputString)
                }
            })
        return userInput
    }
}