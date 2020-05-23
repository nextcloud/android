package com.nextcloud.client.files

import org.junit.Test

class DeepLinkHandlerTest {

    @Test
    fun valid_uri_can_be_handled_by_one_user() {
        // GIVEN
        //      uri matching allowed pattern
        //      one user can open the file

        // WHEN
        //      deep link is handled

        // THEN
        //      file is opened immediately
    }

    @Test
    fun valid_uri_can_be_handled_by_multiple_users() {
        // GIVEN
        //      uri matching allowed pattern
        //      multiple users can open the file

        // WHEN
        //      deep link is handled

        // THEN
        //      user chooser dialog is opened
    }

    @Test
    fun valid_uri_cannot_be_handled_by_any_user() {
        // GIVEN
        //      uri matching allowed pattern
        //      no user can open given uri

        // WHEN
        //      deep link is handled

        // THEN
        //      deep link is ignored
    }

    @Test
    fun invalid_uri_is_ignored() {
        // GIVEN
        //      file uri does not match allowed pattern

        // WHEN
        //      deep link is handled

        // THEN
        //      deep link is ignored
    }
}
