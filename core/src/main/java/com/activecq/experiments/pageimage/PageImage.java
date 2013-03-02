/*
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

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;
import com.day.cq.wcm.foundation.Image;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;

/**
 * User: david
 */
public class PageImage {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public static final String WIDTH = "width";
    public static final String HEIGHT = "height";

    public static final String PAGE_IMAGES_RESOURCE_NAME = "image";
    public static final String PAGE_IMAGES_RESOURCE_NAME_PREFIX = "image-";

    public static final String BASE_IMG_SELECTOR = ".page.img";

    private final Image image;

    /**
     *
     * @param page
     * @param aspectRatio
     * @param width
     * @param height
     */
    public PageImage(final Page page, final String aspectRatio, final int width, final int height) {

        final Resource pageContentResource = page.getContentResource();
        final Style style = pageContentResource.adaptTo(Style.class);

        final Resource cropResource = this.getCropImageResource(page, aspectRatio);
        final Resource imageResource = cropResource.getParent();
        final ValueMap imageProperties = imageResource.adaptTo(ValueMap.class);

        this.image = new Image(cropResource);

        this.image.set(Image.PN_REFERENCE, imageProperties.get(Image.PN_REFERENCE, String.class));

        this.image.loadStyleData(style);
        this.image.setSelector(getSelectors(width, height));
        this.image.setSuffix(this.getTimestamp(cropResource, imageResource) + image.getExtension());
       /*
        if (!currentDesign.equals(resourceDesign)) {
            image.setSuffix(currentDesign.getId());
        }
        */
    }

    /**
     *
     * @return
     */
    public Image getImage() {
        return this.image;
    }

    /**
     *
     * @return
     */
    public String getSrc() {
        return this.image.getSrc();
    }

    /**
     *
     * @return
     */
    public String getAlt() {
        // Change this to display alt-text from DAM Asset or Page
        return this.image.getAlt();
    }

    /**
     *
     * @param width
     * @param height
     * @return
     */
    private String getSelectors(final int width, final int height) {
        String selectors = BASE_IMG_SELECTOR;

        if(width > 0) {
            selectors += "." + WIDTH + "." + String.valueOf(width);
        }

        if(height > 0) {
            selectors += "." + HEIGHT + "." + String.valueOf(height);
        }

        return selectors;
    }

    /**
     *
     * @param page
     * @param aspectRatio
     * @return
     */
    private Resource getCropImageResource(final Page page, final String aspectRatio) {
        final ResourceResolver resourceResolver = page.getContentResource().getResourceResolver();
        final String path = page.getContentResource().getPath() + "/" + PAGE_IMAGES_RESOURCE_NAME + "/" + PAGE_IMAGES_RESOURCE_NAME_PREFIX + aspectRatio;
        return resourceResolver.resolve(path);
    }

    /**
     *
     * @param imageResource
     * @param cropResource
     * @return
     */
    private String getTimestamp(final Resource imageResource, final Resource cropResource) {
        final ResourceResolver resourceResolver = imageResource.getResourceResolver();
        final ValueMap imageProperties = imageResource.adaptTo(ValueMap.class);
        final ValueMap cropProperties = cropResource.adaptTo(ValueMap.class);

        final String fileReferencePath = imageProperties.get(Image.PN_REFERENCE, String.class);
        final Resource fileReferenceResource = resourceResolver.resolve(fileReferencePath);

        final Calendar imageTime = imageProperties.get(JcrConstants.JCR_LASTMODIFIED, imageProperties.get(JcrConstants.JCR_CREATED, Calendar.class));
        final Calendar cropTime = cropProperties.get(JcrConstants.JCR_LASTMODIFIED, cropProperties.get(JcrConstants.JCR_CREATED, Calendar.class));
        Calendar fileReferenceTime = null;

        if(fileReferenceResource != null) {
            final ValueMap fileReferenceProperties = fileReferenceResource.adaptTo(ValueMap.class);
            fileReferenceTime = fileReferenceProperties.get(JcrConstants.JCR_LASTMODIFIED, fileReferenceProperties.get(JcrConstants.JCR_CREATED, Calendar.class));
        }

        Long timestamp = imageTime.getTimeInMillis() + cropTime.getTimeInMillis();
        if(fileReferenceTime != null) {
            timestamp += fileReferenceTime.getTimeInMillis();
        }

        return String.valueOf(timestamp);
    }

}
