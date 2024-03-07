package com.kb714.i18nrailstextextractor

import com.kb714.i18nrailstextextractor.processors.FileProcessor
import com.kb714.i18nrailstextextractor.processors.RbFileProcessor
import com.kb714.i18nrailstextextractor.processors.ErbFileProcessor

object FileProcessorFactory {
    fun getProcessor(fileExtension: String?): FileProcessor? {
        return when (fileExtension) {
            "rb" -> RbFileProcessor()
            "erb" -> ErbFileProcessor()
            // quizás sea bueno agregar archivos como jbuilder
            else -> null
        }
    }
}