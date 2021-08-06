package com.nextcloud.android.sso;

import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.util.EncodingUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Vector;

public class PatchMethod extends PostMethod {
    // -------------------------------------------------------------- Constants

    /**
     * Log object for this class.
     */
    private static final Log LOG = LogFactory.getLog(PatchMethod.class);

    /**
     * The buffered request body consisting of <code>NameValuePair</code>s.
     */
    private Vector params = new Vector();

    // ----------------------------------------------------------- Constructors

    /**
     * No-arg constructor.
     */
    public PatchMethod() {
        super();
    }

    /**
     * Constructor specifying a URI.
     *
     * @param uri either an absolute or relative URI
     */
    public PatchMethod(String uri) {
        super(uri);
    }

    // ----------------------------------------------------- Instance Methods


    /**
     * Returns <tt>"POST"</tt>.
     *
     * @return <tt>"POST"</tt>
     * @since 2.0
     */
    @Override
    public String getName() {
        return "PATCH";
    }


    /**
     * Returns <tt>true</tt> if there is a request body to be sent.
     *
     * <P>This method must be overwritten by sub-classes that implement
     * alternative request content input methods
     * </p>
     *
     * @return boolean
     * @since 2.0beta1
     */
    protected boolean hasRequestContent() {
        LOG.trace("enter PatchMethod.hasRequestContent()");
        if (!this.params.isEmpty()) {
            return true;
        } else {
            return super.hasRequestContent();
        }
    }


    /**
     * Clears request body.
     *
     * <p>This method must be overwritten by sub-classes that implement
     * alternative request content input methods</p>
     *
     * @since 2.0beta1
     */
    protected void clearRequestBody() {
        LOG.trace("enter PatchMethod.clearRequestBody()");
        this.params.clear();
        super.clearRequestBody();
    }


    /**
     * Generates a request entity from the patch parameters, if present.  Calls {@link
     * EntityEnclosingMethod#generateRequestBody()} if parameters have not been set.
     *
     * @since 3.0
     */
    protected RequestEntity generateRequestEntity() {
        if (!this.params.isEmpty()) {
            // Use a ByteArrayRequestEntity instead of a StringRequestEntity.
            // This is to avoid potential encoding issues.  Form url encoded strings
            // are ASCII by definition but the content type may not be.  Treating the content
            // as bytes allows us to keep the current charset without worrying about how
            // this charset will effect the encoding of the form url encoded string.
            String content = EncodingUtil.formUrlEncode(getParameters(), getRequestCharSet());
            ByteArrayRequestEntity entity = new ByteArrayRequestEntity(
                EncodingUtil.getAsciiBytes(content),
                FORM_URL_ENCODED_CONTENT_TYPE
            );
            return entity;
        } else {
            return super.generateRequestEntity();
        }
    }
}
