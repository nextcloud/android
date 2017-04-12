package third_parties.ezvcard_android;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ezvcard.property.VCardProperty;

/*
 Copyright (c) 2014-2015, Michael Angstadt
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 The views and conclusions contained in the software and documentation are those
 of the authors and should not be interpreted as representing official policies,
 either expressed or implied, of the FreeBSD Project.
 */

/**
 * Represents an "X-ANDROID-CUSTOM" property.
 * @author Michael Angstadt
 */
public class AndroidCustomField extends VCardProperty {
    private String type;
    private boolean dir;
    private List<String> values = new ArrayList<String>();

    /**
     * Creates an "item" field.
     * @param type the type
     * @param value the value
     * @return the property
     */
    public static AndroidCustomField item(String type, String value) {
        AndroidCustomField property = new AndroidCustomField();
        property.dir = false;
        property.type = type;
        property.values.add(value);
        return property;
    }

    /**
     * Creates a "dir" field.
     * @param type the type
     * @param values the values
     * @return the property
     */
    public static AndroidCustomField dir(String type, String... values) {
        AndroidCustomField property = new AndroidCustomField();
        property.dir = true;
        property.type = type;
        Collections.addAll(property.values, values);
        return property;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getValues() {
        return values;
    }

    public boolean isDir() {
        return dir;
    }

    public void setDir(boolean dir) {
        this.dir = dir;
    }

    public boolean isItem() {
        return !isDir();
    }

    public void setItem(boolean item) {
        setDir(!item);
    }

    public boolean isNickname() {
        return "nickname".equals(type);
    }

    public boolean isContactEvent() {
        return "contact_event".equals(type);
    }

    public boolean isRelation() {
        return "relation".equals(type);
    }
}
