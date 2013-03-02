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
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.designer.Design;
import com.day.cq.wcm.api.designer.Designer;
import com.day.cq.wcm.api.designer.Style;
import com.day.cq.wcm.foundation.Image;
import org.apache.commons.lang.StringUtils;
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
    private final boolean renderable;
    private String alt;

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

        Long timestamp = this.getTimestamp(cropResource, imageResource);
        timestamp += this.getDesignHash(imageResource);

        this.image.setSuffix(String.valueOf(timestamp) + image.getExtension());

        this.renderable = (cropResource != null && imageResource != null && image.hasContent());

        this.alt = this.getAltFromPage(page, this.image.getAlt());
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
        return this.alt;
    }

    /**
     * ./altText
     * description
     * pagetitle
     * title
     * navtitle
     * default
     *
     * @param page
     * @param defaultAlt
     * @return
     */
    private String getAltFromPage(final Page page, final String defaultAlt) {
        if(StringUtils.isNotBlank(page.getProperties().get(Image.PN_ALT, String.class))) {
            return page.getProperties().get(Image.PN_ALT, String.class);
        } else if(StringUtils.isNotBlank(page.getDescription())) {
            return page.getDescription();
        } else if (StringUtils.isNotBlank(page.getPageTitle())) {
            return page.getPageTitle();
        } else if (StringUtils.isNotBlank(page.getTitle())) {
            return page.getTitle();
        } else if (StringUtils.isNotBlank(page.getNavigationTitle())) {
            return page.getNavigationTitle();
        } else {
            return defaultAlt;
        }
    }

    public void setAlt(String alt) {
        this.alt = alt;
    }

    public boolean hasContent() {
        return this.renderable;
    }

    public boolean hasError() {
        return !this.hasContent();
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
    private Long getTimestamp(final Resource imageResource, final Resource cropResource) {
        final ResourceResolver resourceResolver = imageResource.getResourceResolver();
        Calendar imageTime = null;
        Calendar cropTime = null;
        Calendar fileReferenceTime = null;

        String fileReferencePath = null;

        if(imageResource != null) {
            final ValueMap imageProperties = imageResource.adaptTo(ValueMap.class);
            if(imageProperties != null) {
                fileReferencePath = imageProperties.get(Image.PN_REFERENCE, String.class);
                imageTime = imageProperties.get(JcrConstants.JCR_LASTMODIFIED, imageProperties.get(JcrConstants.JCR_CREATED, Calendar.class));
            }
        }

        if(cropResource != null) {
            final ValueMap cropProperties = cropResource.adaptTo(ValueMap.class);
            if(cropProperties != null) {
                cropTime = cropProperties.get(JcrConstants.JCR_LASTMODIFIED, cropProperties.get(JcrConstants.JCR_CREATED, Calendar.class));
            }
        }

        if(StringUtils.isNotBlank(fileReferencePath)) {
            final Resource fileReferenceResource = resourceResolver.resolve(fileReferencePath);

            if(fileReferenceResource != null) {
                if(fileReferenceResource != null) {
                    final ValueMap fileReferenceProperties = fileReferenceResource.adaptTo(ValueMap.class);
                    if(fileReferenceProperties != null) {
                        fileReferenceTime = fileReferenceProperties.get(JcrConstants.JCR_LASTMODIFIED, fileReferenceProperties.get(JcrConstants.JCR_CREATED, Calendar.class));
                    }
                }
            }
        }

        Long timestamp = 0L;
        timestamp += (imageTime != null) ? imageTime.getTimeInMillis() : 0L;
        timestamp += (cropTime != null) ? cropTime.getTimeInMillis() : 0L;
        timestamp += (fileReferenceTime != null) ? fileReferenceTime.getTimeInMillis() : 0L;

        return timestamp;
    }

    private Long getDesignHash(final Resource resource) {
        final ResourceResolver resourceResolver = resource.getResourceResolver();
        final PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
        final Page page = pageManager.getContainingPage(resource);

        if(page == null) { return 0L; }

        final Designer designer = resourceResolver.adaptTo(Designer.class);
        final Design design = designer.getDesign(page);

        return new Long(design.getId().hashCode());
    }
}
