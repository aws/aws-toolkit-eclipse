/*
 * Copyright 2009-2012 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package com.amazonaws.eclipse.core.ui.overview;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.PreferenceLinkListener;
import com.amazonaws.eclipse.core.ui.WebLinkListener;

/**
 * UI toolkit for the AWS Toolkit Overview view components to facilitate
 * creating links, labels, sections, etc.
 */
public class Toolkit {

    /** Shared overview resources (images, fonts, colors, etc) */
    private OverviewResources resources;

    /**
     * Sets the shared OverviewResources object this toolkit object will use for
     * referencing shared resources like images, fonts, colors, etc.
     *
     * @param resources
     *            The shared overview resources object.
     */
    void setResources(OverviewResources resources) {
        this.resources = resources;
    }

    /**
     * Creates and returns a new Label within the specified parent, with the
     * specified text.
     *
     * @param parent
     *            The parent composite in which to create the new label.
     * @param text
     *            The text to display in the label.
     * @return The new label.
     */
    public static Label newLabel(Composite parent, String text) {
        Label l = new Label(parent, SWT.NONE);
        l.setText(text);
        l.setBackground(parent.getBackground());

        return l;
    }

    /**
     * Creates and returns a new Link, which when selected, will run the
     * specified action.
     *
     * @param parent
     *            The parent composite in which to create the new link.
     * @param text
     *            The text to display in the link.
     * @param action
     *            The action to execute when the Link is selected.
     * @return The new link.
     */
    public static Link newActionLink(Composite parent, String text, final IAction action) {
        Link link = new Link(parent, SWT.NONE);
        link.setText(createAnchor(text, text));
        link.setBackground(parent.getBackground());

        link.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                action.run();
            }
        });

        return link;
    }

    /**
     * Creates and returns a new Link, which when selected, will open the
     * specified preference page.
     *
     * @param parent
     *            The parent composite in which to create the new link.
     * @param text
     *            The text to display in the link.
     * @param target
     *            The ID of the preference page to display when this link is
     *            selected.
     * @return The new link.
     */
    public static Link newPreferenceLink(Composite parent, String text, String target) {
        Link link = new Link(parent, SWT.NONE);
        link.setText(createAnchor(text, target));
        link.setBackground(parent.getBackground());

        link.addListener(SWT.Selection, new PreferenceLinkListener());

        return link;
    }

    /**
     * Creates and returns a new Link, which when selected, will open a browser
     * to a URL in the href attribute of an included anchor tag. Note that this
     * method requires the caller to include an HTML anchor tag in the text they
     * pass, and won't add the HTML anchor tag to the text like other methods in
     * this class will.
     *
     * @param parent
     *            The parent composite in which to create the new link.
     * @param text
     *            The text to display in the link, including an HTML anchor tag
     *            where the anchor's href attribute indicates what HTTP URL
     *            should be opened when selected.
     * @return The new link.
     */
    public static Link newWebLink(Composite parent, String text) {
        Link link = new Link(parent, SWT.NONE);
        link.setText(text);
        link.setBackground(parent.getBackground());
        link.addListener(SWT.Selection, new WebLinkListener());

        return link;
    }

    /**
     * Creates and returns a new Link, which when selected, will open the
     * Eclipse help system to the specified target help topic of the form
     * "/<plugin-id>/<path-to-help-doc>" (ex:
     * "/com.amazonaws.eclipse.ec2/html/foo/help.html").
     *
     * @param parent
     *            The parent composite in which to create the new link.
     * @param text
     *            The text to display in the link.
     * @param target
     *            The Eclipse help topic doc that should be opened when the link
     *            is selected.
     * @return The new link.
     */
    public static Link newHelpLink(Composite parent, String text, String target) {
       Link link = new Link(parent, SWT.NONE);
       link.setText(createAnchor(text, target));
       link.setBackground(parent.getBackground());

       link.addListener(SWT.Selection, new Listener() {
           @Override
        public void handleEvent(Event event) {
              PlatformUI.getWorkbench().getHelpSystem().displayHelpResource(event.text);
           }
       });

       return link;
    }

    /**
     * Creates and returns a link, which when selected, will open a browser to
     * the specified URL.
     *
     * @param parent
     *            The parent composite in which to create the new link.
     * @param text
     *            The text to display for the link.
     * @param target
     *            The HTTP URL to open when the link is selected.
     * @return The new link.
     */
    public static Link newWebLink(Composite parent, String text, String target) {
        return newWebLink(parent, createAnchor(text, target));
    }

    /**
     * Creates and returns a link, which when selected, will run the specified
     * IActionDelegate.
     *
     * @param parent
     *            The parent composite in which to create the new link.
     * @param text
     *            The text to display for the link.
     * @param delegate
     *            The delegate object to run when the link is selected.
     * @return The new link.
     */
    public static Link newActionDelegateLink(Composite parent, String text,
            final IActionDelegate delegate) {

        final Action proxy = new Action("runAction") {
            @Override
            public void run() {
                delegate.run(this);
            }
        };

        Link link = new Link(parent, SWT.NONE);
        link.setText(createAnchor(text, text));
        link.setBackground(parent.getBackground());

        link.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                proxy.run();
            }
        });

        return link;
    }

    /**
     * Wraps the specified text in an HTML anchor tag, with the href attribute
     * set to the specified target.
     *
     * @param text
     *            The text to wrap in an HTML anchor tag.
     * @param target
     *            The URL to set in the href attribute of the anchor tag.
     * @return The specified text wrapped in an anchor tag.
     */
    public static String createAnchor(String text, String target) {
        return "<a href=\"" + target + "\">" + text + "</a>";
    }

    /**
     * Creates a new label with the image from AwsToolkitCore's ImageRegistry
     * identified by the specified image ID.
     *
     * @param parent
     *            The parent composite in which to create the new label.
     * @param imageId
     *            The ID of the image in AwsToolkitCore's ImageRegistry to
     *            display.
     */
    public static Label newImageLabel(Composite parent, String imageId) {
        Label label = new Label(parent, SWT.NONE);
        label.setImage(AwsToolkitCore.getDefault().getImageRegistry().get(imageId));
        label.setBackground(parent.getBackground());

        return label;
    }

    public ImageHyperlink newListItem(Composite parent, String text, String href) {
        return newListItem(parent, text, href, null);
    }
    /**
     * Creates a new list item in the specified parent composite. Displays a
     * bulleted list item containing a hyperlink with the specified text and
     * href target, and an optional associated default action to execute if the
     * href target doesn't match any of the basic handlers for web links,
     * Eclipse preference links, Eclipse help links, etc.
     *
     * @param parent
     *            The parent composite in which the new list item will be
     *            created.
     * @param text
     *            The text for the list item hyperlink.
     * @param href
     *            The hyperlink href target for the new list item.
     * @param defaultAction
     *            The default action to run if the target href doesn't match any
     *            of the standard supported prefixes ('http', 'help:',
     *            'preference:').
     *
     * @return The new link in the list item.
     */
    public ImageHyperlink newListItem(Composite parent, String text,
            String href, final IAction defaultAction) {
        FormToolkit formToolkit = resources.getFormToolkit();

        Composite composite = formToolkit.createComposite(parent);
        TableWrapLayout layout = LayoutUtils.newSlimTableWrapLayout(2);
        layout.leftMargin = 5;
        composite.setLayout(layout);
        composite.setLayoutData(new TableWrapData(TableWrapData.FILL));

        Composite bulletComposite = formToolkit.createComposite(composite);
        GridLayout bulletLayout = new GridLayout();
        bulletLayout.marginTop = 8;
        bulletLayout.marginHeight = 0;
        bulletComposite.setLayout(bulletLayout);

        Label bullet = formToolkit.createLabel(bulletComposite, null);
        bullet.setImage(resources.getImage(OverviewResources.IMAGE_BULLET));
        TableWrapData layoutData = new TableWrapData(TableWrapData.LEFT, TableWrapData.MIDDLE);
        layoutData.grabVertical = true;
        bullet.setLayoutData(new GridData());

        ImageHyperlink link = formToolkit.createImageHyperlink(composite, SWT.RIGHT | SWT.NO_FOCUS | SWT.WRAP);
        link.setText(text);
        link.setHref(href);
        link.setUnderlined(false);
        link.setLayoutData(new TableWrapData(TableWrapData.LEFT, TableWrapData.TOP));
        link.setFont(resources.getFont("text"));

        // Any external links should be displayed with the external link image
        if (href != null && href.startsWith("http")) {
            link.setImage(OverviewResources.getExternalLinkImage());
        }

        /*
         * Always add a hyperlink listener for the basic action prefixes
         * ('http:', 'help:', 'preference:', etc.) and optionally add a default
         * listener for a custom action.
         */
        link.addHyperlinkListener(new HyperlinkHandler());
        if (defaultAction != null) {
            link.addHyperlinkListener(new HyperlinkAdapter() {
                @Override
                public void linkActivated(HyperlinkEvent e) {
                    defaultAction.run();
                }
            });
        }

        return link;
    }

    /**
     * Creates a new sub-section in the specified parent composite with the
     * default sub section font and color.
     *
     * @param parent
     *            The parent composite for the new sub section.
     * @param title
     *            The section title.
     *
     * @return The composite within the new section, ready for the caller to
     *         populate with widgets.
     */
    public Composite newSubSection(Composite parent, String title) {
        return newSubSection(parent, title,
                resources.getColor("module-subheader"),
                resources.getFont("module-subheader"));
    }

    /**
     * Creates a new sub-section in the specified parent composite with
     * explicitly specified font and color.
     *
     * @param parent
     *            The parent composite for the new sub section.
     * @param title
     *            The section title.
     * @param color
     *            The color for the sub-section title text.
     * @param font
     *            The font for the sub-section title text.
     *
     * @return The composite within the new section, ready for the caller to
     *         populate with widgets.
     */
    public Composite newSubSection(Composite parent, String title, Color color, Font font) {
        FormToolkit formToolkit = resources.getFormToolkit();

        Section section = formToolkit.createSection(parent, Section.EXPANDED);
        section.setText(title);
        section.setFont(font);
        section.setForeground(color);
        TableWrapData tableWrapData = new TableWrapData(TableWrapData.FILL);
        tableWrapData.grabHorizontal = true;
        section.setLayoutData(tableWrapData);

        Composite composite = formToolkit.createComposite(section);
        TableWrapLayout layout = new TableWrapLayout();
        layout.leftMargin = 2;
        layout.rightMargin = 2;
        layout.topMargin = 0;
        layout.bottomMargin = 0;
        layout.verticalSpacing = 0;
        composite.setLayout(layout);
        section.setClient(composite);
        section.setLayoutData(new TableWrapData(TableWrapData.FILL));

        return composite;
    }

}
