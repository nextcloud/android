package third_parties.ezvcard_android;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.graphics.Bitmap;
import android.os.RemoteException;
import android.provider.ContactsContract;

import com.owncloud.android.utils.DisplayUtils;

import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ezvcard.VCard;
import ezvcard.parameter.ImageType;
import ezvcard.property.Address;
import ezvcard.property.Birthday;
import ezvcard.property.Email;
import ezvcard.property.FormattedName;
import ezvcard.property.Impp;
import ezvcard.property.Nickname;
import ezvcard.property.Note;
import ezvcard.property.Organization;
import ezvcard.property.Photo;
import ezvcard.property.RawProperty;
import ezvcard.property.StructuredName;
import ezvcard.property.Telephone;
import ezvcard.property.Title;
import ezvcard.property.Url;
import ezvcard.property.VCardProperty;
import ezvcard.util.TelUri;

import static android.text.TextUtils.isEmpty;

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
 * Inserts a {@link VCard} into an Android database.
 *
 * @author Pratyush
 * @author Michael Angstadt
 */
public class ContactOperations {
    private static final int rawContactID = 0;

    private final Context context;
    private final NonEmptyContentValues account;

    public ContactOperations(Context context) {
        this(context, null, null);
    }

    public ContactOperations(Context context, String accountName, String accountType) {
        this.context = context;

        account = new NonEmptyContentValues();
        account.put(ContactsContract.RawContacts.ACCOUNT_TYPE, accountType);
        account.put(ContactsContract.RawContacts.ACCOUNT_NAME, accountName);
    }

    public void insertContact(VCard vcard) throws RemoteException, OperationApplicationException {
        // TODO handle Raw properties - Raw properties include various extension which start with "X-" like X-ASSISTANT, X-AIM, X-SPOUSE

        List<NonEmptyContentValues> contentValues = new ArrayList<NonEmptyContentValues>();
        convertName(contentValues, vcard);
        convertNickname(contentValues, vcard);
        convertPhones(contentValues, vcard);
        convertEmails(contentValues, vcard);
        convertAddresses(contentValues, vcard);
        convertIms(contentValues, vcard);

        // handle Android Custom fields..This is only valid for Android generated Vcards. As the Android would
        // generate NickName, ContactEvents other than Birthday and RelationShip with this "X-ANDROID-CUSTOM" name
        convertCustomFields(contentValues, vcard);

        // handle Iphone kinda of group properties. which are grouped together.
        convertGroupedProperties(contentValues, vcard);

        convertBirthdays(contentValues, vcard);

        convertWebsites(contentValues, vcard);
        convertNotes(contentValues, vcard);
        convertPhotos(contentValues, vcard);
        convertOrganization(contentValues, vcard);

        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>(contentValues.size());
        ContentValues cv = account.getContentValues();
        //ContactsContract.RawContact.CONTENT_URI needed to add account, backReference is also not needed
        ContentProviderOperation operation =
                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                        .withValues(cv)
                        .build();
        operations.add(operation);
        for (NonEmptyContentValues values : contentValues) {
            cv = values.getContentValues();
            if (cv.size() == 0) {
                continue;
            }

            //@formatter:off
            operation =
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactID)
                            .withValues(cv)
                            .build();
            //@formatter:on
            operations.add(operation);
        }

        // Executing all the insert operations as a single database transaction
        context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, operations);
    }

    public void updateContact(VCard vcard, Long key) throws RemoteException, OperationApplicationException {

        List<NonEmptyContentValues> contentValues = new ArrayList<NonEmptyContentValues>();
        convertName(contentValues, vcard);
        convertNickname(contentValues, vcard);
        convertPhones(contentValues, vcard);
        convertEmails(contentValues, vcard);
        convertAddresses(contentValues, vcard);
        convertIms(contentValues, vcard);

        // handle Android Custom fields..This is only valid for Android generated Vcards. As the Android would
        // generate NickName, ContactEvents other than Birthday and RelationShip with this "X-ANDROID-CUSTOM" name
        convertCustomFields(contentValues, vcard);

        // handle Iphone kinda of group properties. which are grouped together.
        convertGroupedProperties(contentValues, vcard);

        convertBirthdays(contentValues, vcard);

        convertWebsites(contentValues, vcard);
        convertNotes(contentValues, vcard);
        convertPhotos(contentValues, vcard);
        convertOrganization(contentValues, vcard);

        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>(contentValues.size());
        ContentValues cv = account.getContentValues();
        //ContactsContract.RawContact.CONTENT_URI needed to add account, backReference is also not needed
        long contactID = key;
        ContentProviderOperation operation;

        for (NonEmptyContentValues values : contentValues) {
            cv = values.getContentValues();
            if (cv.size() == 0) {
                continue;
            }

            String mimeType = cv.getAsString("mimetype");
            cv.remove("mimetype");
            //@formatter:off
            operation =
                    ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                            .withSelection(ContactsContract.Data.RAW_CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ? ", new String[]{"" + contactID, "" + mimeType})
                            .withValues(cv)
                            .build();
            //@formatter:on
            operations.add(operation);
        }

        // Executing all the insert operations as a single database transaction
        context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, operations);

    }

    private void convertName(List<NonEmptyContentValues> contentValues, VCard vcard) {
        NonEmptyContentValues values = new NonEmptyContentValues(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);

        String firstName = null, lastName = null, namePrefix = null, nameSuffix = null;
        StructuredName n = vcard.getStructuredName();
        if (n != null) {
            firstName = n.getGiven();
            values.put(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, firstName);

            lastName = n.getFamily();
            values.put(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, lastName);

            List<String> prefixes = n.getPrefixes();
            if (!prefixes.isEmpty()) {
                namePrefix = prefixes.get(0);
                values.put(ContactsContract.CommonDataKinds.StructuredName.PREFIX, namePrefix);
            }

            List<String> suffixes = n.getSuffixes();
            if (!suffixes.isEmpty()) {
                nameSuffix = suffixes.get(0);
                values.put(ContactsContract.CommonDataKinds.StructuredName.SUFFIX, nameSuffix);
            }
        }

        FormattedName fn = vcard.getFormattedName();
        String formattedName = (fn == null) ? null : fn.getValue();

        String displayName;
        if (isEmpty(formattedName)) {
            StringBuilder sb = new StringBuilder();
            if (!isEmpty(namePrefix)){
                sb.append(namePrefix).append(' ');
            }
            if (!isEmpty(firstName)){
                sb.append(firstName).append(' ');
            }
            if (!isEmpty(lastName)){
                sb.append(lastName).append(' ');
            }
            if (!isEmpty(nameSuffix)){
                if (sb.length() > 0){
                    sb.deleteCharAt(sb.length()-1); //delete space character
                    sb.append(", ");
                }
                sb.append(nameSuffix);
            }

            displayName = sb.toString().trim();
        } else {
            displayName = formattedName;
        }
        values.put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName);

        RawProperty xPhoneticFirstName = vcard.getExtendedProperty("X-PHONETIC-FIRST-NAME");
        String firstPhoneticName = (xPhoneticFirstName == null) ? null : xPhoneticFirstName.getValue();
        values.put(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_GIVEN_NAME, firstPhoneticName);

        RawProperty xPhoneticLastName = vcard.getExtendedProperty("X-PHONETIC-LAST-NAME");
        String lastPhoneticName = (xPhoneticLastName == null) ? null : xPhoneticLastName.getValue();
        values.put(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_FAMILY_NAME, lastPhoneticName);

        contentValues.add(values);
    }

    private void convertNickname(List<NonEmptyContentValues> contentValues, VCard vcard) {
        for (Nickname nickname : vcard.getNicknames()) {
            List<String> nicknameValues = nickname.getValues();
            if (nicknameValues.isEmpty()) {
                continue;
            }

            for (String nicknameValue : nicknameValues) {
                NonEmptyContentValues cv = new NonEmptyContentValues(ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE);
                cv.put(ContactsContract.CommonDataKinds.Nickname.NAME, nicknameValue);
                contentValues.add(cv);
            }
        }
    }

    private void convertPhones(List<NonEmptyContentValues> contentValues, VCard vcard) {
        for (Telephone telephone : vcard.getTelephoneNumbers()) {
            String value = telephone.getText();
            TelUri uri = telephone.getUri();
            if (isEmpty(value)) {
                if (uri == null) {
                    continue;
                }
                value = uri.toString();
            }

            int phoneKind = DataMappings.getPhoneType(telephone);

            NonEmptyContentValues cv = new NonEmptyContentValues(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
            cv.put(ContactsContract.CommonDataKinds.Phone.NUMBER, value);
            cv.put(ContactsContract.CommonDataKinds.Phone.TYPE, phoneKind);
            contentValues.add(cv);
        }
    }

    private void convertEmails(List<NonEmptyContentValues> contentValues, VCard vcard) {
        for (Email email : vcard.getEmails()) {
            String value = email.getValue();
            if (isEmpty(value)) {
                continue;
            }

            int emailKind = DataMappings.getEmailType(email);

            NonEmptyContentValues cv = new NonEmptyContentValues(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
            cv.put(ContactsContract.CommonDataKinds.Email.ADDRESS, value);
            cv.put(ContactsContract.CommonDataKinds.Email.TYPE, emailKind);
            contentValues.add(cv);
        }
    }

    private void convertAddresses(List<NonEmptyContentValues> contentValues, VCard vcard) {
        for (Address address : vcard.getAddresses()) {
            NonEmptyContentValues cv = new NonEmptyContentValues(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE);

            String street = address.getStreetAddress();
            cv.put(ContactsContract.CommonDataKinds.StructuredPostal.STREET, street);

            String poBox = address.getPoBox();
            cv.put(ContactsContract.CommonDataKinds.StructuredPostal.POBOX, poBox);

            String city = address.getLocality();
            cv.put(ContactsContract.CommonDataKinds.StructuredPostal.CITY, city);

            String state = address.getRegion();
            cv.put(ContactsContract.CommonDataKinds.StructuredPostal.REGION, state);

            String zipCode = address.getPostalCode();
            cv.put(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE, zipCode);

            String country = address.getCountry();
            cv.put(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY, country);

            String label = address.getLabel();
            cv.put(ContactsContract.CommonDataKinds.StructuredPostal.LABEL, label);

            int addressKind = DataMappings.getAddressType(address);
            cv.put(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, addressKind);

            contentValues.add(cv);
        }
    }

    private void convertIms(List<NonEmptyContentValues> contentValues, VCard vcard) {
        //handle extended properties
        for (Map.Entry<String, Integer> entry : DataMappings.getImPropertyNameMappings().entrySet()) {
            String propertyName = entry.getKey();
            Integer protocolType = entry.getValue();
            List<RawProperty> rawProperties = vcard.getExtendedProperties(propertyName);
            for (RawProperty rawProperty : rawProperties) {
                NonEmptyContentValues cv = new NonEmptyContentValues(ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE);

                String value = rawProperty.getValue();
                cv.put(ContactsContract.CommonDataKinds.Im.DATA, value);

                cv.put(ContactsContract.CommonDataKinds.Im.PROTOCOL, protocolType);

                contentValues.add(cv);
            }
        }

        //handle IMPP properties
        for (Impp impp : vcard.getImpps()) {
            NonEmptyContentValues cv = new NonEmptyContentValues(ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE);

            String immpAddress = impp.getHandle();
            cv.put(ContactsContract.CommonDataKinds.Im.DATA, immpAddress);

            int immpProtocolType = DataMappings.getIMTypeFromProtocol(impp.getProtocol());
            cv.put(ContactsContract.CommonDataKinds.Im.PROTOCOL, immpProtocolType);

            contentValues.add(cv);
        }
    }

    private void convertCustomFields(List<NonEmptyContentValues> contentValues, VCard vcard) {
        for (AndroidCustomField customField : vcard.getProperties(AndroidCustomField.class)) {
            List<String> values = customField.getValues();
            if (values.isEmpty()) {
                continue;
            }

            NonEmptyContentValues cv;
            if (customField.isNickname()) {
                cv = new NonEmptyContentValues(ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE);
                cv.put(ContactsContract.CommonDataKinds.Nickname.NAME, values.get(0));
            } else if (customField.isContactEvent()) {
                cv = new NonEmptyContentValues(ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE);
                cv.put(ContactsContract.CommonDataKinds.Event.START_DATE, values.get(0));
                cv.put(ContactsContract.CommonDataKinds.Event.TYPE, values.get(1));
            } else if (customField.isRelation()) {
                cv = new NonEmptyContentValues(ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE);
                cv.put(ContactsContract.CommonDataKinds.Relation.NAME, values.get(0));
                cv.put(ContactsContract.CommonDataKinds.Relation.TYPE, values.get(1));
            } else {
                continue;
            }

            contentValues.add(cv);
        }
    }

    private void convertGroupedProperties(List<NonEmptyContentValues> contentValues, VCard vcard) {
        List<RawProperty> extendedProperties = vcard.getExtendedProperties();
        Map<String, List<RawProperty>> orderedByGroup = orderPropertiesByGroup(extendedProperties);
        final int ABDATE = 1, ABRELATEDNAMES = 2;

        for (List<RawProperty> properties : orderedByGroup.values()) {
            if (properties.size() == 1) {
                continue;
            }

            String label = null;
            String val = null;
            int mime = 0;
            for (RawProperty property : properties) {
                String name = property.getPropertyName();

                if ("X-ABDATE".equalsIgnoreCase(name)) {
                    label = property.getValue(); //date
                    mime = ABDATE;
                    continue;
                }

                if ("X-ABRELATEDNAMES".equalsIgnoreCase(name)) {
                    label = property.getValue(); //name
                    mime = ABRELATEDNAMES;
                    continue;
                }

                if ("X-ABLABEL".equalsIgnoreCase(name)) {
                    val = property.getValue(); // type of value ..Birthday,anniversary
                    continue;
                }
            }

            NonEmptyContentValues cv = null;
            switch (mime) {
                case ABDATE:
                    cv = new NonEmptyContentValues(ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE);

                    cv.put(ContactsContract.CommonDataKinds.Event.START_DATE, label);

                    int type = DataMappings.getDateType(val);
                    cv.put(ContactsContract.CommonDataKinds.Event.TYPE, type);

                    break;

                case ABRELATEDNAMES:
                    if (val != null) {
                        cv = new NonEmptyContentValues(ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE);
                        cv.put(ContactsContract.CommonDataKinds.Nickname.NAME, label);

                        if (!"Nickname".equals(val)) {
                            type = DataMappings.getNameType(val);
                            cv.put(ContactsContract.CommonDataKinds.Relation.TYPE, type);
                        }
                    }

                    break;

                default:
                    continue;
            }

            contentValues.add(cv);
        }
    }

    private void convertBirthdays(List<NonEmptyContentValues> contentValues, VCard vcard) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        for (Birthday birthday : vcard.getBirthdays()) {
            Date date = birthday.getDate();
            if (date == null) {
                continue;
            }

            NonEmptyContentValues cv = new NonEmptyContentValues(ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE);
            cv.put(ContactsContract.CommonDataKinds.Event.TYPE, ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY);
            cv.put(ContactsContract.CommonDataKinds.Event.START_DATE, df.format(date));
            contentValues.add(cv);
        }
    }

    private void convertWebsites(List<NonEmptyContentValues> contentValues, VCard vcard) {
        for (Url url : vcard.getUrls()) {
            String urlValue = url.getValue();
            if (isEmpty(urlValue)) {
                continue;
            }

            int type = DataMappings.getWebSiteType(url.getType());

            NonEmptyContentValues cv = new NonEmptyContentValues(ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE);
            cv.put(ContactsContract.CommonDataKinds.Website.URL, urlValue);
            cv.put(ContactsContract.CommonDataKinds.Website.TYPE, type);
            contentValues.add(cv);
        }
    }

    private void convertNotes(List<NonEmptyContentValues> contentValues, VCard vcard) {
        for (Note note : vcard.getNotes()) {
            String noteValue = note.getValue();

            NonEmptyContentValues cv = new NonEmptyContentValues(ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE);
            cv.put(ContactsContract.CommonDataKinds.Note.NOTE, noteValue);
            contentValues.add(cv);
        }
    }

    private void convertPhotos(List<NonEmptyContentValues> contentValues, VCard vcard) {
        for (Photo photo : vcard.getPhotos()) {
            if (photo.getUrl() != null) {
                downloadPhoto(photo);
            }
            byte[] data = photo.getData();

            NonEmptyContentValues cv = new NonEmptyContentValues(ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE);
            cv.put(ContactsContract.CommonDataKinds.Photo.PHOTO, data);
            contentValues.add(cv);
        }
    }

    private void downloadPhoto(Photo photo) {
        String url = photo.getUrl();
        Bitmap bitmap = DisplayUtils.downloadImageSynchronous(context, url);
        if (bitmap != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] bitmapdata = stream.toByteArray();
            photo.setData(bitmapdata, ImageType.find(null, null,
                    url.substring(url.lastIndexOf(".") + 1)));
        }
    }

    private void convertOrganization(List<NonEmptyContentValues> contentValues, VCard vcard) {
        NonEmptyContentValues cv = new NonEmptyContentValues(ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE);

        Organization organization = vcard.getOrganization();
        if (organization != null) {
            List<String> values = organization.getValues();
            String keys[] = { ContactsContract.CommonDataKinds.Organization.COMPANY, ContactsContract.CommonDataKinds.Organization.DEPARTMENT, ContactsContract.CommonDataKinds.Organization.OFFICE_LOCATION };
            for (int i = 0; i < values.size(); i++) {
                String key = keys[i];
                String value = values.get(i);
                cv.put(key, value);
            }
        }

        List<Title> titleList = vcard.getTitles();
        if (!titleList.isEmpty()) {
            cv.put(ContactsContract.CommonDataKinds.Organization.TITLE, titleList.get(0).getValue());
        }

        contentValues.add(cv);
    }

    /**
     * Groups properties by their group name.
     *
     * @param properties the properties to group
     * @return a map where the key is the group name (null for no group) and the value is the list of properties that
     * belong to that group
     */
    private <T extends VCardProperty> Map<String, List<T>> orderPropertiesByGroup(Iterable<T> properties) {
        Map<String, List<T>> groupedProperties = new HashMap<String, List<T>>();

        for (T property : properties) {
            String group = property.getGroup();
            if (isEmpty(group)) {
                continue;
            }

            List<T> groupPropertiesList = groupedProperties.get(group);
            if (groupPropertiesList == null) {
                groupPropertiesList = new ArrayList<T>();
                groupedProperties.put(group, groupPropertiesList);
            }
            groupPropertiesList.add(property);
        }

        return groupedProperties;
    }

    /**
     * A wrapper for {@link ContentValues} that only adds values which are
     * non-null and non-empty (in the case of Strings).
     */
    private static class NonEmptyContentValues {
        private final ContentValues contentValues = new ContentValues();
        private final String contentItemType;

        public NonEmptyContentValues() {
            this(null);
        }

        /**
         * @param contentItemType the MIME type (value of
         * {@link ContactsContract.Contacts.Data#MIMETYPE})
         */
        public NonEmptyContentValues(String contentItemType) {
            this.contentItemType = contentItemType;
        }

        public void put(String key, String value) {
            if (isEmpty(value)) {
                return;
            }
            contentValues.put(key, value);
        }

        public void put(String key, int value) {
            contentValues.put(key, value);
        }

        public void put(String key, byte[] value) {
            if (value == null) {
                return;
            }
            contentValues.put(key, value);
        }

        /**
         * Gets the wrapped {@link ContentValues} object, adding the MIME type
         * entry if the values map is not empty.
         * @return the wrapped {@link ContentValues} object
         */
        public ContentValues getContentValues() {
            if (contentValues.size() > 0 && contentItemType != null) {
                put(ContactsContract.Contacts.Data.MIMETYPE, contentItemType);
            }
            return contentValues;
        }
    }
}
