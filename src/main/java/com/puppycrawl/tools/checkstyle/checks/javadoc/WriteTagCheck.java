////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2020 the original author or authors.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////

package com.puppycrawl.tools.checkstyle.checks.javadoc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.puppycrawl.tools.checkstyle.StatelessCheck;
import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FileContents;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import com.puppycrawl.tools.checkstyle.api.TextBlock;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.utils.CommonUtil;

/**
 * <p>
 * Requires user defined Javadoc tag to be present in Javadoc comment with defined format.
 * To define the format for a tag, set property tagFormat to a regular expression.
 * Property tagSeverity is used for severity of events when the tag exists.
 * </p>
 * <ul>
 * <li>
 * Property {@code tag} - Specify the name of tag.
 * Default value is {@code null}.
 * </li>
 * <li>
 * Property {@code tagFormat} - Specify the regexp to match tag content.
 * Default value is {@code null}.
 * </li>
 * <li>
 * Property {@code tagSeverity} - Specify the severity level when tag is found and printed.
 * Default value is {@code info}.
 * </li>
 * <li>
 * Property {@code tokens} - tokens to check
 * Default value is:
 * <a href="https://checkstyle.org/apidocs/com/puppycrawl/tools/checkstyle/api/TokenTypes.html#INTERFACE_DEF">
 * INTERFACE_DEF</a>,
 * <a href="https://checkstyle.org/apidocs/com/puppycrawl/tools/checkstyle/api/TokenTypes.html#CLASS_DEF">
 * CLASS_DEF</a>,
 * <a href="https://checkstyle.org/apidocs/com/puppycrawl/tools/checkstyle/api/TokenTypes.html#ENUM_DEF">
 * ENUM_DEF</a>,
 * <a href="https://checkstyle.org/apidocs/com/puppycrawl/tools/checkstyle/api/TokenTypes.html#ANNOTATION_DEF">
 * ANNOTATION_DEF</a>.
 * </li>
 * </ul>
 * <p>
 * To configure the check for printing author name:
 * </p>
 * <pre>
 * &lt;module name="WriteTag"&gt;
 *   &lt;property name="tag" value="@author"/&gt;
 *   &lt;property name="tagFormat" value="\S"/&gt;
 * &lt;/module&gt;
 * </pre>
 * <p>
 * To configure the check to print warnings if an "@incomplete" tag is found,
 * and not print anything if it is not found:
 * </p>
 * <pre>
 * &lt;module name="WriteTag"&gt;
 *   &lt;property name="tag" value="@incomplete"/&gt;
 *   &lt;property name="tagFormat" value="\S"/&gt;
 *   &lt;property name="severity" value="ignore"/&gt;
 *   &lt;property name="tagSeverity" value="warning"/&gt;
 * &lt;/module&gt;
 * </pre>
 *
 * @since 4.2
 */
@StatelessCheck
public class WriteTagCheck
    extends AbstractCheck {

    /**
     * A key is pointing to the warning message text in "messages.properties"
     * file.
     */
    public static final String MSG_MISSING_TAG = "type.missingTag";

    /**
     * A key is pointing to the warning message text in "messages.properties"
     * file.
     */
    public static final String MSG_WRITE_TAG = "javadoc.writeTag";

    /**
     * A key is pointing to the warning message text in "messages.properties"
     * file.
     */
    public static final String MSG_TAG_FORMAT = "type.tagFormat";

    /** Compiled regexp to match tag. */
    private Pattern tagRegExp;
    /** Specify the regexp to match tag content. */
    private Pattern tagFormat;

    /** Specify the name of tag. */
    private String tag;
    /** Specify the severity level when tag is found and printed. */
    private SeverityLevel tagSeverity = SeverityLevel.INFO;

    /**
     * Setter to specify the name of tag.
     *
     * @param tag tag to check
     */
    public void setTag(String tag) {
        this.tag = tag;
        tagRegExp = CommonUtil.createPattern(tag + "\\s*(.*$)");
    }

    /**
     * Setter to specify the regexp to match tag content.
     *
     * @param pattern a {@code String} value
     */
    public void setTagFormat(Pattern pattern) {
        tagFormat = pattern;
    }

    /**
     * Setter to specify the severity level when tag is found and printed.
     *
     * @param severity  The new severity level
     * @see SeverityLevel
     */
    public final void setTagSeverity(SeverityLevel severity) {
        tagSeverity = severity;
    }

    @Override
    public int[] getDefaultTokens() {
        return new int[] {TokenTypes.INTERFACE_DEF,
                          TokenTypes.CLASS_DEF,
                          TokenTypes.ENUM_DEF,
                          TokenTypes.ANNOTATION_DEF,
        };
    }

    @Override
    public int[] getAcceptableTokens() {
        return new int[] {TokenTypes.INTERFACE_DEF,
                          TokenTypes.CLASS_DEF,
                          TokenTypes.ENUM_DEF,
                          TokenTypes.ANNOTATION_DEF,
                          TokenTypes.METHOD_DEF,
                          TokenTypes.CTOR_DEF,
                          TokenTypes.ENUM_CONSTANT_DEF,
                          TokenTypes.ANNOTATION_FIELD_DEF,
        };
    }

    @Override
    public int[] getRequiredTokens() {
        return CommonUtil.EMPTY_INT_ARRAY;
    }

    @Override
    public void visitToken(DetailAST ast) {
        final FileContents contents = getFileContents();
        final int lineNo = ast.getLineNo();
        final TextBlock cmt =
            contents.getJavadocBefore(lineNo);
        if (cmt == null) {
            log(ast, MSG_MISSING_TAG, tag);
        }
        else {
            checkTag(ast, cmt.getText());
        }
    }

    /**
     * Verifies that a type definition has a required tag.
     * @param ast the DetailAST instance for which the Javadoc comment is associated
     * @param comment the Javadoc comment for the type definition.
     */
    private void checkTag(DetailAST ast, String... comment) {
        if (tagRegExp != null) {
            boolean hasTag = false;
            for (int i = 0; i < comment.length; i++) {
                final String commentValue = comment[i];
                final Matcher matcher = tagRegExp.matcher(commentValue);
                if (matcher.find()) {
                    hasTag = true;
                    final int contentStart = matcher.start(1);
                    final String content = commentValue.substring(contentStart);
                    if (tagFormat == null || tagFormat.matcher(content).find()) {
                        logTag(ast.getLineNo() + i - comment.length, ast.getColumnNo(),
                                tag, content);
                    }
                    else {
                        log(ast.getLineNo() + i - comment.length, ast.getColumnNo(),
                                MSG_TAG_FORMAT, tag, tagFormat.pattern());
                    }
                }
            }
            if (!hasTag) {
                log(ast, MSG_MISSING_TAG, tag);
            }
        }
    }

    /**
     * Log a message.
     *
     * @param line the line number where the violation was found
     * @param column the column number where the violation was found
     * @param tagName the javadoc tag to be logged
     * @param tagValue the contents of the tag
     *
     * @see java.text.MessageFormat
     */
    private void logTag(int line, int column, String tagName, String tagValue) {
        final String originalSeverity = getSeverity();
        setSeverity(tagSeverity.getName());

        log(line, column, MSG_WRITE_TAG, tagName, tagValue);

        setSeverity(originalSeverity);
    }

}
