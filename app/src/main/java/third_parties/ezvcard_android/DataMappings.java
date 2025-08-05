/*
 * SPDX-FileCopyrightText: 2014-2015, Michael Angstadt, All rights reserved
 * SPDX-License-Identifier: BSD-2-Clause
 */
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
        phoneTypeMappings = Map.ofEntries(Map.entry(TelephoneType.BBS, ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM), Map.entry(TelephoneType.CAR, ContactsContract.CommonDataKinds.Phone.TYPE_CAR), Map.entry(TelephoneType.CELL, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE), Map.entry(TelephoneType.FAX, ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME), Map.entry(TelephoneType.HOME, ContactsContract.CommonDataKinds.Phone.TYPE_HOME), Map.entry(TelephoneType.ISDN, ContactsContract.CommonDataKinds.Phone.TYPE_ISDN), Map.entry(TelephoneType.MODEM, ContactsContract.CommonDataKinds.Phone.TYPE_OTHER), Map.entry(TelephoneType.PAGER, ContactsContract.CommonDataKinds.Phone.TYPE_PAGER), Map.entry(TelephoneType.MSG, ContactsContract.CommonDataKinds.Phone.TYPE_MMS), Map.entry(TelephoneType.PCS, ContactsContract.CommonDataKinds.Phone.TYPE_OTHER), Map.entry(TelephoneType.TEXT, ContactsContract.CommonDataKinds.Phone.TYPE_MMS), Map.entry(TelephoneType.TEXTPHONE, ContactsContract.CommonDataKinds.Phone.TYPE_MMS), Map.entry(TelephoneType.VIDEO, ContactsContract.CommonDataKinds.Phone.TYPE_OTHER), Map.entry(TelephoneType.WORK, ContactsContract.CommonDataKinds.Phone.TYPE_WORK), Map.entry(TelephoneType.VOICE, ContactsContract.CommonDataKinds.Phone.TYPE_OTHER));
    }

    private static final Map<String, Integer> websiteTypeMappings;
    static {
        websiteTypeMappings = Map.of("home", ContactsContract.CommonDataKinds.Website.TYPE_HOME, "work", ContactsContract.CommonDataKinds.Website.TYPE_WORK, "homepage", ContactsContract.CommonDataKinds.Website.TYPE_HOMEPAGE, "profile", ContactsContract.CommonDataKinds.Website.TYPE_PROFILE);
    }

    private static final Map<EmailType, Integer> emailTypeMappings;
    static {
        emailTypeMappings = Map.of(EmailType.HOME, ContactsContract.CommonDataKinds.Email.TYPE_HOME, EmailType.WORK, ContactsContract.CommonDataKinds.Email.TYPE_WORK);
    }

    private static final Map<AddressType, Integer> addressTypeMappings;
    static {
        Map<AddressType, Integer> m = new HashMap<>();
        m.put(AddressType.HOME, ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME);
        m.put(AddressType.get("business"), ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK);
        m.put(AddressType.WORK, ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK);
        m.put(AddressType.get("other"), ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER);
        addressTypeMappings = Collections.unmodifiableMap(m);
    }

    private static final Map<String, Integer> abRelatedNamesMappings;
    static {
        abRelatedNamesMappings = Map.of("father", ContactsContract.CommonDataKinds.Relation.TYPE_FATHER, "spouse", ContactsContract.CommonDataKinds.Relation.TYPE_SPOUSE, "mother", ContactsContract.CommonDataKinds.Relation.TYPE_MOTHER, "brother", ContactsContract.CommonDataKinds.Relation.TYPE_BROTHER, "parent", ContactsContract.CommonDataKinds.Relation.TYPE_PARENT, "sister", ContactsContract.CommonDataKinds.Relation.TYPE_SISTER, "child", ContactsContract.CommonDataKinds.Relation.TYPE_CHILD, "assistant", ContactsContract.CommonDataKinds.Relation.TYPE_ASSISTANT, "partner", ContactsContract.CommonDataKinds.Relation.TYPE_PARTNER, "manager", ContactsContract.CommonDataKinds.Relation.TYPE_MANAGER);
    }

    private static final Map<String, Integer> abDateMappings;
    static {
        abDateMappings = Map.of("anniversary", ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY, "other", ContactsContract.CommonDataKinds.Event.TYPE_OTHER);
    }

    private static final Map<String, Integer> imPropertyNameMappings;
    static{
        imPropertyNameMappings = Map.ofEntries(Map.entry("X-AIM", ContactsContract.CommonDataKinds.Im.PROTOCOL_AIM), Map.entry("X-ICQ", ContactsContract.CommonDataKinds.Im.PROTOCOL_ICQ), Map.entry("X-QQ", ContactsContract.CommonDataKinds.Im.PROTOCOL_ICQ), Map.entry("X-GOOGLE-TALK", ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM), Map.entry("X-JABBER", ContactsContract.CommonDataKinds.Im.PROTOCOL_JABBER), Map.entry("X-MSN", ContactsContract.CommonDataKinds.Im.PROTOCOL_MSN), Map.entry("X-MS-IMADDRESS", ContactsContract.CommonDataKinds.Im.PROTOCOL_MSN), Map.entry("X-YAHOO", ContactsContract.CommonDataKinds.Im.PROTOCOL_YAHOO), Map.entry("X-SKYPE", ContactsContract.CommonDataKinds.Im.PROTOCOL_SKYPE), Map.entry("X-SKYPE-USERNAME", ContactsContract.CommonDataKinds.Im.PROTOCOL_SKYPE), Map.entry("X-TWITTER", ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM));
    }

    private static final Map<String, Integer> imProtocolMappings;
    static{
        imProtocolMappings = Map.of("aim", ContactsContract.CommonDataKinds.Im.PROTOCOL_AIM, "icq", ContactsContract.CommonDataKinds.Im.PROTOCOL_ICQ, "msn", ContactsContract.CommonDataKinds.Im.PROTOCOL_MSN, "ymsgr", ContactsContract.CommonDataKinds.Im.PROTOCOL_YAHOO, "skype", ContactsContract.CommonDataKinds.Im.PROTOCOL_SKYPE);
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
