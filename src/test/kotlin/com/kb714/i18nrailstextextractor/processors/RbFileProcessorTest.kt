package com.kb714.i18nrailstextextractor.processors

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase

class RbFileProcessorTest : BasePlatformTestCase() {
    private val rbProcessor = RbFileProcessor()

    fun `test should extract simple variables`(){
        val text = "text with #{foo} #{bar}"
        val (transformedText, variables) = rbProcessor.transformTextForI18nOnRuby(text)

        val expectedMap = mapOf("foo" to "foo", "bar" to "bar")
        val expectedText = "text with %{foo} %{bar}"

        TestCase.assertEquals(expectedMap, variables)
        TestCase.assertEquals(expectedText, transformedText)
    }

    fun `test should extract class and methods`(){
        val text = "text with #{Foo.method(:attribute)} or #{Foo::Bar.method(:attribute)}"
        val (transformedText, variables) = rbProcessor.transformTextForI18nOnRuby(text)

        val expectedMap = mapOf(
                "foo_method" to "Foo.method(:attribute)",
                "foo_bar_method" to "Foo::Bar.method(:attribute)"
        )
        val expectedText = "text with %{foo_method} or %{foo_bar_method}"

        TestCase.assertEquals(expectedMap, variables)
        TestCase.assertEquals(expectedText, transformedText)
    }

    fun `test should extract mix variables`(){
        val text = "#{variable} #{Foo.method(:attribute)} or #{Foo::Bar.method[:attribute]} #{other_variable}"
        val (transformedText, variables) = rbProcessor.transformTextForI18nOnRuby(text)

        val expectedMap = mapOf(
                "variable" to "variable",
                "other_variable" to "other_variable",
                "foo_method" to "Foo.method(:attribute)",
                "foo_bar_method" to "Foo::Bar.method[:attribute]",
        )
        val expectedText = "%{variable} %{foo_method} or %{foo_bar_method} %{other_variable}"

        TestCase.assertEquals(expectedMap, variables)
        TestCase.assertEquals(expectedText, transformedText)
    }
}