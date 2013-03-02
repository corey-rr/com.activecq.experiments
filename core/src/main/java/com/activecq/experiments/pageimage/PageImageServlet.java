/*
 * Copyright 1997-2008 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 *
 * -----------------------------------------------------------------------
 *
 * Copyright 2012 david gonzalez.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.activecq.experiments.pageimage;

import com.day.cq.commons.ImageHelper;
import com.day.cq.commons.ImageResource;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.designer.Design;
import com.day.cq.wcm.api.designer.Designer;
import com.day.cq.wcm.api.designer.Style;
import com.day.cq.wcm.commons.AbstractImageServlet;
import com.day.cq.wcm.foundation.Image;
import com.day.image.Layer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.ListIterator;


/**
 * Writes a Page Image to the response; Allows for selector-based re-sizing.
 */
public class PageImageServlet extends AbstractImageServlet {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public static final String STYLE_PROPERTY_ALLOWED_DIMENSIONS = "allowedDimensions";

    private static final String PNG_IMAGE_MIMETYPE = "image/png";
    private static final String GIF_IMAGE_MIMETYPE = "image/gif";
    private static final String DEFAULT_IMAGE_MIMETYPE = PNG_IMAGE_MIMETYPE;

    @Override
    protected Layer createLayer(ImageContext c)
            throws RepositoryException, IOException {
        return null;
    }

    @Override
    protected ImageResource createImageResource(Resource resource) {
        return new Image(resource);
    }

    @Override
    protected void writeLayer(SlingHttpServletRequest slingRequest,
                              SlingHttpServletResponse slingResponse,
                              ImageContext imageContext, Layer layer)
            throws IOException, RepositoryException {

        final Image image = new Image(imageContext.resource);

        final String width = this.getDimension(PageImage.WIDTH, slingRequest);
        final String height = this.getDimension(PageImage.HEIGHT, slingRequest);

        final Resource pageImage = imageContext.resource.getParent();
        final ValueMap pageImageProperties = pageImage.adaptTo(ValueMap.class);

        image.set(Image.PN_REFERENCE, pageImageProperties.get(Image.PN_REFERENCE, String.class));

        if (isAllowedDimension(width, height, imageContext.resource)) {
            image.set(Image.PN_WIDTH, width);
            image.set(Image.PN_HEIGHT, height);
        }

        if (!image.hasContent()) {
            slingResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // get pure layer
        layer = image.getLayer(false, false, false);
        boolean modified = false;

        if (layer != null) {
            // crop
            modified = image.crop(layer) != null;

            // rotate
            modified |= image.rotate(layer) != null;

            // resize
            modified |= image.resize(layer) != null;

            // apply diff if needed (because we create the layer inline)
            modified |= applyDiff(layer, imageContext);
        }

        if (modified) {
            // If the image is modified then adjust accordingly and write to the output stream
            String mimeType = image.getMimeType();

            if (ImageHelper.getExtensionFromType(mimeType) == null) {
                mimeType = DEFAULT_IMAGE_MIMETYPE;
            }

            slingResponse.setContentType(mimeType);
            layer.write(mimeType, mimeType.equals(GIF_IMAGE_MIMETYPE) ? 255 : 1.0, slingResponse.getOutputStream());
        } else {
            // Image has not been modified (crop, rotate, size) so simply original image data to output stream
            final Property data = image.getData();
            final InputStream in = data.getBinary().getStream();

            slingResponse.setContentLength((int) data.getLength());
            slingResponse.setContentType(image.getMimeType());

            IOUtils.copy(in, slingResponse.getOutputStream());
            in.close();
        }

        slingResponse.flushBuffer();
    }

    /**
     * Retrieves the Width and Height from the Sling Selectors on the requested URI
     *
     * @param key
     * @param request
     * @return
     */
    private String getDimension(final String key, final SlingHttpServletRequest request) {
        final RequestPathInfo rpi = request.getRequestPathInfo();
        final ListIterator<String> iterator = Arrays.asList(rpi.getSelectors()).listIterator();

        while (iterator.hasNext()) {
            final String selector = iterator.next();

            if (key.equals(selector)) {
                return iterator.next();
            }
        }

        return "0";
    }

    /**
     * Checks if the dimensions requested by the URL are
     * @param width
     * @param height
     * @param imageResource
     * @return
     */
    private boolean isAllowedDimension(final String width, final String height, final Resource imageResource) {
        final ResourceResolver resourceResolver = imageResource.getResourceResolver();
        final PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
        final Page page = pageManager.getContainingPage(imageResource);

        if(page == null) { return true; }

        final Designer designer = resourceResolver.adaptTo(Designer.class);
        final Design design = designer.getDesign(page);

        if(design == null) { return true; }

        final Style style = design.getStyle(imageResource);

        if(style == null) { return true; }

        final String[] dimensions = style.get(STYLE_PROPERTY_ALLOWED_DIMENSIONS, new String[]{});

        if (ArrayUtils.isEmpty(dimensions)) { return true; }

        for (final String dimension : dimensions) {
            final String requestedDimension = width + "x" + height;
            if (StringUtils.equals(requestedDimension, dimension)) {
                return true;
            }
        }

        return false;
    }



}