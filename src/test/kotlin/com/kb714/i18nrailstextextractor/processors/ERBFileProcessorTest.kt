package com.kb714.i18nrailstextextractor.processors

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase

class ERBFileProcessorTest : BasePlatformTestCase() {
    private val erbProcessor = ErbFileProcessor()

    fun `test should extract common variables`() {
        val text = "text with <%= variable %>"
        val (transformedText, variables) = erbProcessor.transformTextForI18nOnHTML(text)

        val expectedMap = mapOf("variable" to "variable")
        val expectedText = "text with %{variable}"

        TestCase.assertEquals(expectedMap, variables)
        TestCase.assertEquals(expectedText, transformedText)
    }


    fun `test should extract repeat variables`() {
        val text = "text with <%= variable %> and <%= variable %>"
        val (transformedText, variables) = erbProcessor.transformTextForI18nOnHTML(text)

        val expectedMap = mapOf("variable" to "variable")
        val expectedText = "text with %{variable} and %{variable}"

        TestCase.assertEquals(expectedMap, variables)
        TestCase.assertEquals(expectedText, transformedText)
    }

    fun `test should extract multiple common variables`() {
        val text = "text with <%= variable %>, <%= foo %> and <%= bar %>"
        val (transformedText, variables) = erbProcessor.transformTextForI18nOnHTML(text)

        val expectedMap = mapOf(
            "variable" to "variable",
            "foo" to "foo",
            "bar" to "bar"
        )
        val expectedText = "text with %{variable}, %{foo} and %{bar}"

        TestCase.assertEquals(expectedMap, variables)
        TestCase.assertEquals(expectedText, transformedText)
    }

    fun `test should extract common variables in HTML`() {
        val text = """<div class="something">text with <%= variable %>, <%= foo %> and <%= bar %></div>"""
        val (transformedText, variables) = erbProcessor.transformTextForI18nOnHTML(text)

        val expectedMap = mapOf(
            "variable" to "variable",
            "foo" to "foo",
            "bar" to "bar"
        )
        val expectedText = """<div class="something">text with %{variable}, %{foo} and %{bar}</div>"""

        TestCase.assertEquals(expectedMap, variables)
        TestCase.assertEquals(expectedText, transformedText)
    }

    fun `test should extract mix variables in HTML`(){
        val text = """
            <div class="foo" data-title="<%= title %>" style="<%= something_else == :attr ? 'text_one' : 'text_two' %>">
                <%= variable %> <p><%= Foo.method(:attribute) %></p> or 
                <%= Foo::Bar.method[:attribute] %> <%= other_variable %>"
            </div>
            """
        val (transformedText, variables) = erbProcessor.transformTextForI18nOnHTML(text)

        val expectedMap = mapOf(
            "title" to "title",
            "something_else_attr_text_one_text_two" to "something_else == :attr ? 'text_one' : 'text_two'",
            "variable" to "variable",
            "foo_method" to "Foo.method(:attribute)",
            "foo_bar_method" to "Foo::Bar.method[:attribute]",
            "other_variable" to "other_variable"
        )
        val expectedText = """
            <div class="foo" data-title="%{title}" style="%{something_else_attr_text_one_text_two}">
                %{variable} <p>%{foo_method}</p> or 
                %{foo_bar_method} %{other_variable}"
            </div>
            """

        TestCase.assertEquals(expectedMap, variables)
        TestCase.assertEquals(expectedText, transformedText)
    }
}