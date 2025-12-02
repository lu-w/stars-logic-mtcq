package tools.aqua.stars.logic.mtcq

import DLConvertible
import openllet.aterm.ATermAppl
import openllet.core.KnowledgeBase
import openllet.core.utils.TermFactory.literal
import openllet.core.utils.TermFactory.term
import kotlin.reflect.KProperty1
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties

fun Any.isDLConvertible(dlConvertibleClasses: Set<Any>): Boolean =
    this::class.hasAnnotation<DLConvertible>() || this::class in dlConvertibleClasses

/**
 * Adds an object that has an @DLConvertible annotation to the given knowledge base using some fancy magic (it simply
 * iterates over all its properties and recursively adds those to the knowledge base as well).
 * Alternatively, it looks at all classes contained in dlConvertibleClasses (if annotations are not possible).
 * prefix: An optional prefix to add in front of any created class, individual, and property.
 */
fun Any.addToKB(
    kb: KnowledgeBase, prefix: String = "", visited: MutableSet<Any> = mutableSetOf(),
    dlConvertibleClasses: Set<Any> = setOf()
) {
    // Only process objects marked with @DLConvertible or manually given as dlConvertibleClasses
    val clazz = this::class
    if (!this.isDLConvertible(dlConvertibleClasses)) return
    if (this in visited) return
    visited += this

    val className = prefix + (clazz.simpleName ?: "Anonymous")
    val classTerm = term(className)
    kb.addClass(classTerm)

    val ind = term(prefix + objectId(this))
    kb.addIndividual(ind)
    kb.addType(ind, classTerm)

    for (prop in clazz.memberProperties) {
        val value = (prop as KProperty1<Any, *>).get(this) ?: continue
        val propTerm = term(prefix + prop.name)
        when (value) {
            is Iterable<*> -> value.forEach { item ->
                if (item is String || item is Number || item is Boolean)
                    addDataProperty(kb, propTerm, ind, item)
                else if (item != null && item.isDLConvertible(dlConvertibleClasses))
                    addObjectProperty(kb, prefix, visited, propTerm, ind, item)
            }

            is String, is Number, is Boolean ->
                addDataProperty(kb, propTerm, ind, value)

            else ->
                if (value.isDLConvertible(dlConvertibleClasses))
                    addObjectProperty(kb, prefix, visited, propTerm, ind, value, dlConvertibleClasses)
        }
    }
}

private fun addObjectProperty(
    kb: KnowledgeBase,
    prefix: String,
    visited: MutableSet<Any>,
    propTerm: ATermAppl,
    subj: ATermAppl,
    obj: Any,
    dlConvertibleClasses: Set<Any> = setOf()
) {
    obj.addToKB(kb, prefix, visited, dlConvertibleClasses = dlConvertibleClasses)
    kb.addObjectProperty(propTerm)
    kb.addPropertyValue(propTerm, subj, term(prefix + objectId(obj)))
}

private fun addDataProperty(
    kb: KnowledgeBase,
    propTerm: ATermAppl,
    subj: ATermAppl,
    obj: Any
) {
    kb.addDatatypeProperty(propTerm)
    kb.addPropertyValue(propTerm, subj, toData(obj))
}

private fun toData(obj: Any): ATermAppl {
    return when (obj) {
        is String -> literal(obj)
        is Float -> literal(obj)
        is Double -> literal(obj)
        is Int -> literal(obj)
        is Boolean -> literal(obj)
        else -> literal(obj.toString())
    }
}

private fun objectId(obj: Any): String {
    val clazz = obj::class
    val id = try {
        @Suppress("UNCHECKED_CAST")
        (clazz.memberProperties.first { it.name == "id" } as KProperty1<Any, *>).get(obj)?.toString()
    } catch (e: Exception) {
        obj.hashCode()
    }
    return "${clazz.simpleName}_$id"
}