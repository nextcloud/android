/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.annotation

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.FIELD,
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.EXPRESSION,
)
@Retention(AnnotationRetention.SOURCE)
/* Used to highlight changes in core code
* Alternatives:
*   - for layouts use - 'app:ionosCustomization=""'
*   - comment with text '<IONOS Customization>' where other options is not applicable
* */
annotation class IonosCustomization(val value: String = "")