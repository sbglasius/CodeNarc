/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codenarc.rule.unnecessary

import org.codehaus.groovy.ast.*
import org.codenarc.rule.AbstractAstVisitor
import org.codenarc.rule.AbstractAstVisitorRule
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * The 'public' modifier is not required on methods, constructors or classes.
 *
 * @author Hamlet D'Arcy
 * @author Chris Mair
 */
class UnnecessaryPublicModifierRule extends AbstractAstVisitorRule {

    String name = 'UnnecessaryPublicModifier'
    int priority = 3
    Class astVisitorClass = UnnecessaryPublicModifierAstVisitor
}

class UnnecessaryPublicModifierAstVisitor extends AbstractAstVisitor {

    private static final Logger LOG = LoggerFactory.getLogger(UnnecessaryPublicModifierAstVisitor)

    @Override
    protected void visitClassEx(ClassNode node) {
        if (!node.isScript()) {
            checkDeclaration(node, node.name, 'classes')
        }
        super.visitClassEx(node)
    }

    @Override
    void visitMethodEx(MethodNode node) {
        if (node.genericsTypes == null || node.genericsTypes.length == 0) {
            checkDeclaration(node, node.name, 'methods')
        }
        super.visitMethodEx(node)
    }

    @Override
    void visitConstructor(ConstructorNode node) {
        checkDeclaration(node, node.name, 'constructors')
        super.visitConstructor(node)
    }

    private void checkDeclaration(ASTNode node, String nodeName, String nodeType) {
        String declaration = getDeclaration(node)
        // remove node name from the declaration just in case it has ' public ' in the name
        declaration = declaration?.replace(nodeName, '')
        if (declaration?.startsWith('public ') || declaration?.contains(' public ')) {
            addViolation(node, "The public keyword is unnecessary for $nodeType")
        }
    }

    private String getDeclaration(ASTNode node) {
        if (node.lineNumber < 0) {
            return ''
        }

        def current = node.lineNumber - 1
        def startColumn = 0

        if (node instanceof AnnotatedNode && !node.annotations.empty) {
            // Start checking from the end of the last annotation
            def lastAnnotation = node.annotations.last()
            current = lastAnnotation.lastLineNumber - 1
            startColumn = lastAnnotation.lastColumnNumber - 1
        }

        String acc = ''
        while (current <= node.lastLineNumber) {
            def line = sourceCode.lines[current]    // do not trim()
            if (line == null) {
                if (node.isAbstract()) {
                    return acc // can happen with abstract method in Parrot parser
                }
                LOG.warn("${rule.name} cannot find source code line $current in ${sourceCode.name}. Scanning lines ${node.lineNumber} to ${node.lastLineNumber}.")
                return ''
            } else if (line.contains('{')) {
                return acc + line[startColumn..(line.indexOf('{'))]
            }
            acc = acc + line.substring(startColumn) + ' '
            startColumn = 0
            current++
        }
        acc
    }
}
