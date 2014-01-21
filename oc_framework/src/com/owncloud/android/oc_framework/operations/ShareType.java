/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2014 ownCloud (http://www.owncloud.org/)
 *   
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *   
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *   
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS 
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN 
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */

package com.owncloud.android.oc_framework.operations;

/**
 * Enum for Share Type, with values:
 * -1 - No shared
 *  0 - Shared by user
 *  1 - Shared by group
 *  3 - Shared by public link
 *  4 - Shared by e-mail
 *  5 - Shared by contact
 *  
 * @author masensio
 *
 */

public enum ShareType {
    NO_SHARED (-1),
    USER (0),
    GROUP (1),
    PUBLIC_LINK (3),
    EMAIL (4),
    CONTACT (5);
    
    private int value;
    
    private ShareType(int value)
    {
        this.value = value;
    }
    
    public int getValue() {
        return value;
    }
    
    public static ShareType fromValue(int value)
    {
        switch (value)
        {
        case -1:
            return NO_SHARED;
        case 0:
            return USER;
        case 1:
            return GROUP;
        case 3:
            return PUBLIC_LINK;
        case 4:
            return EMAIL;
        case 5:
            return CONTACT;
        }
        return null;
    }
};