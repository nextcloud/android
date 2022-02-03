package third_parties.ezvcard_android;

import android.provider.ContactsContract;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import ezvcard.parameter.AddressType;
import ezvcard.parameter.EmailType;
import ezvcard.parameter.TelephoneType;
import ezvcard.property.Address;
import ezvcard.property.Email;
import ezvcard.property.Impp;
import ezvcard.property.Telephone;

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
 * Maps between vCard contact data types and Android {@link ContactsContract}
 * data types.
 *
 * @author Pratyush
 * @author Julien Garrigou
 * @author Michael Angstadt
 */
public class DataMappings {
    private static final Map<TelephoneType, Integer> phoneTypeMappings;
    static {
        Map<TelephoneType, Integer> m = new HashMap<TelephoneType, Integer>();
        m.put(TelephoneType.BBS, ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM);
        m.put(TelephoneType.CAR, ContactsContract.CommonDataKinds.Phone.TYPE_CAR);
        m.put(TelephoneType.CELL, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
        m.put(TelephoneType.FAX, ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME);
        m.put(TelephoneType.HOME, ContactsContract.CommonDataKinds.Phone.TYPE_HOME);
        m.put(TelephoneType.ISDN, ContactsContract.CommonDataKinds.Phone.TYPE_ISDN);
        m.put(TelephoneType.MODEM, ContactsContract.CommonDataKinds.Phone.TYPE_OTHER);
        m.put(TelephoneType.PAGER, ContactsContract.CommonDataKinds.Phone.TYPE_PAGER);
        m.put(TelephoneType.MSG, ContactsContract.CommonDataKinds.Phone.TYPE_MMS);
        m.put(TelephoneType.PCS, ContactsContract.CommonDataKinds.Phone.TYPE_OTHER);
        m.put(TelephoneType.TEXT, ContactsContract.CommonDataKinds.Phone.TYPE_MMS);
        m.put(TelephoneType.TEXTPHONE, ContactsContract.CommonDataKinds.Phone.TYPE_MMS);
        m.put(TelephoneType.VIDEO, ContactsContract.CommonDataKinds.Phone.TYPE_OTHER);
        m.put(TelephoneType.WORK, ContactsContract.CommonDataKinds.Phone.TYPE_WORK);
        m.put(TelephoneType.VOICE, ContactsContract.CommonDataKinds.Phone.TYPE_OTHER);
        phoneTypeMappings = Collections.unmodifiableMap(m);
    }

    private static final Map<String, Integer> websiteTypeMappings;
    static {
        Map<String, Integer> m = new HashMap<String, Integer>();
        m.put("home", ContactsContract.CommonDataKinds.Website.TYPE_HOME);
        m.put("work", ContactsContract.CommonDataKinds.Website.TYPE_WORK);
        m.put("homepage", ContactsContract.CommonDataKinds.Website.TYPE_HOMEPAGE);
        m.put("profile", ContactsContract.CommonDataKinds.Website.TYPE_PROFILE);
        websiteTypeMappings = Collections.unmodifiableMap(m);
    }

    private static final Map<EmailType, Integer> emailTypeMappings;
    static {
        Map<EmailType, Integer> m = new HashMap<EmailType, Integer>();
        m.put(EmailType.HOME, ContactsContract.CommonDataKinds.Email.TYPE_HOME);
        m.put(EmailType.WORK, ContactsContract.CommonDataKinds.Email.TYPE_WORK);
        emailTypeMappings = Collections.unmodifiableMap(m);
    }

    private static final Map<AddressType, Integer> addressTypeMappings;
    static {
        Map<AddressType, Integer> m = new HashMap<AddressType, Integer>();
        m.put(AddressType.HOME, ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME);
        m.put(AddressType.get("business"), ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK);
        m.put(AddressType.WORK, ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK);
        m.put(AddressType.get("other"), ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER);
        addressTypeMappings = Collections.unmodifiableMap(m);
    }

    private static final Map<String, Integer> abRelatedNamesMappings;
    static {
        Map<String, Integer> m = new HashMap<String, Integer>();
        m.put("father", ContactsContract.CommonDataKinds.Relation.TYPE_FATHER);
        m.put("spouse", ContactsContract.CommonDataKinds.Relation.TYPE_SPOUSE);
        m.put("mother", ContactsContract.CommonDataKinds.Relation.TYPE_MOTHER);
        m.put("brother", ContactsContract.CommonDataKinds.Relation.TYPE_BROTHER);
        m.put("parent", ContactsContract.CommonDataKinds.Relation.TYPE_PARENT);
        m.put("sister", ContactsContract.CommonDataKinds.Relation.TYPE_SISTER);
        m.put("child", ContactsContract.CommonDataKinds.Relation.TYPE_CHILD);
        m.put("assistant", ContactsContract.CommonDataKinds.Relation.TYPE_ASSISTANT);
        m.put("partner", ContactsContract.CommonDataKinds.Relation.TYPE_PARTNER);
        m.put("manager", ContactsContract.CommonDataKinds.Relation.TYPE_MANAGER);
        abRelatedNamesMappings = Collections.unmodifiableMap(m);
    }

    private static final Map<String, Integer> abDateMappings;
    static {
        Map<String, Integer> m = new HashMap<String, Integer>();
        m.put("anniversary", ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY);
        m.put("other", ContactsContract.CommonDataKinds.Event.TYPE_OTHER);
        abDateMappings = Collections.unmodifiableMap(m);
    }

    private static final Map<String, Integer> imPropertyNameMappings;
    static{
        Map<String, Integer> m = new HashMap<String, Integer>();
        m.put("X-AIM", ContactsContract.CommonDataKinds.Im.PROTOCOL_AIM);
        m.put("X-ICQ", ContactsContract.CommonDataKinds.Im.PROTOCOL_ICQ);
        m.put("X-QQ", ContactsContract.CommonDataKinds.Im.PROTOCOL_ICQ);
        m.put("X-GOOGLE-TALK", ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM);
        m.put("X-JABBER", ContactsContract.CommonDataKinds.Im.PROTOCOL_JABBER);
        m.put("X-MSN", ContactsContract.CommonDataKinds.Im.PROTOCOL_MSN);
        m.put("X-MS-IMADDRESS", ContactsContract.CommonDataKinds.Im.PROTOCOL_MSN);
        m.put("X-YAHOO", ContactsContract.CommonDataKinds.Im.PROTOCOL_YAHOO);
        m.put("X-SKYPE", ContactsContract.CommonDataKinds.Im.PROTOCOL_SKYPE);
        m.put("X-SKYPE-USERNAME", ContactsContract.CommonDataKinds.Im.PROTOCOL_SKYPE);
        m.put("X-TWITTER", ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM);
        imPropertyNameMappings = Collections.unmodifiableMap(m);
    }

    private static final Map<String, Integer> imProtocolMappings;
    static{
        Map<String, Integer> m = new HashMap<String, Integer>();
        m.put("aim", ContactsContract.CommonDataKinds.Im.PROTOCOL_AIM);
        m.put("icq", ContactsContract.CommonDataKinds.Im.PROTOCOL_ICQ);
        m.put("msn", ContactsContract.CommonDataKinds.Im.PROTOCOL_MSN);
        m.put("ymsgr", ContactsContract.CommonDataKinds.Im.PROTOCOL_YAHOO);
        m.put("skype", ContactsContract.CommonDataKinds.Im.PROTOCOL_SKYPE);
        imProtocolMappings = Collections.unmodifiableMap(m);
    }

    /**
     * Maps the value of a URL property's TYPE parameter to the appropriate
     * Android {@link ContactsContract.CommonDataKinds.Website} value.
     * @param type the TYPE parameter value (can be null)
     * @return the Android type
     */
    public static int getWebSiteType(String type) {
        if (type == null){
            return ContactsContract.CommonDataKinds.Website.TYPE_CUSTOM;
        }

        type = type.toLowerCase(Locale.ROOT);
        Integer value = websiteTypeMappings.get(type);
        return (value == null) ? ContactsContract.CommonDataKinds.Website.TYPE_CUSTOM : value;
    }

    /**
     * Maps the value of a X-ABLABEL property to the appropriate
     * Android {@link ContactsContract.CommonDataKinds.Event} value.
     * @param type the property value
     * @return the Android type
     */
    public static int getDateType(String type) {
        if (type == null) {
            return ContactsContract.CommonDataKinds.Event.TYPE_OTHER;
        }

        type = type.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, Integer> entry : abDateMappings.entrySet()){
            if (type.contains(entry.getKey())){
                return entry.getValue();
            }
        }
        return ContactsContract.CommonDataKinds.Event.TYPE_OTHER;
    }

    /**
     * Maps the value of a X-ABLABEL property to the appropriate
     * Android {@link ContactsContract.CommonDataKinds.Relation} value.
     * @param type the property value
     * @return the Android type
     */
    public static int getNameType(String type) {
        if (type == null) {
            return ContactsContract.CommonDataKinds.Relation.TYPE_CUSTOM;
        }

        type = type.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, Integer> entry : abRelatedNamesMappings.entrySet()){
            if (type.contains(entry.getKey())){
                return entry.getValue();
            }
        }
        return ContactsContract.CommonDataKinds.Relation.TYPE_CUSTOM;
    }

    /**
     * Gets the mappings that associate an extended property name (e.g. "X-AIM")
     * with its appropriate Android {@link ContactsContract.CommonDataKinds.Im}
     * value.
     * @return the mappings (the key is the property name, the value is the Android value)
     */
    public static Map<String, Integer> getImPropertyNameMappings(){
        return imPropertyNameMappings;
    }

    /**
     * Converts an IM protocol from a {@link Impp} property (e.g. "aim") to the
     * appropriate Android {@link ContactsContract.CommonDataKinds.Im} value.
     * @param protocol the IM protocol (e.g. "aim", can be null)
     * @return the Android value
     */
    public static int getIMTypeFromProtocol(String protocol) {
        if (protocol == null){
            return ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM;
        }

        protocol = protocol.toLowerCase(Locale.ROOT);
        Integer value = imProtocolMappings.get(protocol);
        return (value == null) ? ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM : value;
    }

    /**
     * Determines the appropriate Android
     * {@link ContactsContract.CommonDataKinds.Phone} value for a
     * {@link Telephone} property.
     * @param property the property
     * @return the Android type value
     */
    public static int getPhoneType(Telephone property) {
        for (TelephoneType type : property.getTypes()){
            Integer androidType = phoneTypeMappings.get(type);
            if (androidType != null){
                return androidType;
            }
        }
        return ContactsContract.CommonDataKinds.Phone.TYPE_OTHER;
    }

    /**
     * Determines the appropriate Android
     * {@link ContactsContract.CommonDataKinds.Email} value for an {@link Email}
     * property.
     * @param property the property
     * @return the Android type value
     */
    public static int getEmailType(Email property) {
        for (EmailType type : property.getTypes()){
            Integer androidType = emailTypeMappings.get(type);
            if (androidType != null){
                return androidType;
            }
        }
        return ContactsContract.CommonDataKinds.Email.TYPE_OTHER;
    }

    /**
     * Determines the appropriate Android
     * {@link ContactsContract.CommonDataKinds.StructuredPostal} value for an
     * {@link Address} property.
     * @param property the property
     * @return the Android type value
     */
    public static int getAddressType(Address property) {
        for (AddressType type : property.getTypes()){
            Integer androidType = addressTypeMappings.get(type);
            if (androidType != null){
                return androidType;
            }
        }
        return ContactsContract.CommonDataKinds.StructuredPostal.TYPE_CUSTOM;
    }

    private DataMappings(){
        //hide constructor
    }
}
