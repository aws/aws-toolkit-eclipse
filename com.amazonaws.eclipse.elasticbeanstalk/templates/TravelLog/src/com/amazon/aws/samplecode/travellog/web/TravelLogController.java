/*
 * Copyright 2010-2011 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazon.aws.samplecode.travellog.web;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.amazon.aws.samplecode.travellog.aws.S3PhotoUtil;
import com.amazon.aws.samplecode.travellog.aws.TravelLogSNSManager;
import com.amazon.aws.samplecode.travellog.dao.TravelLogDAO;
import com.amazon.aws.samplecode.travellog.entity.Comment;
import com.amazon.aws.samplecode.travellog.entity.Entry;
import com.amazon.aws.samplecode.travellog.entity.Journal;
import com.amazon.aws.samplecode.travellog.entity.Photo;
import com.amazon.aws.samplecode.travellog.entity.User;
import com.amazon.aws.samplecode.travellog.util.Configuration;
import com.amazon.aws.samplecode.travellog.util.DataExtractor;
import com.amazon.aws.samplecode.travellog.util.DataLoader;

/**
 * This is the core of the TravelLog functionality.  It's a Spring controller implemented
 * using annotations.  Most methods for loading and storing journals, entries, comments and photos
 * are initiated in this class.
 */
@Controller
public class TravelLogController {

    private TravelLogDAO dao;
    private static final Logger logger=Logger.getLogger(TravelLogController.class.getName());


    /**
     * AWS Elastic Beanstalk checks your application's health by periodically
     * sending an HTTP HEAD request to a resource in your application. By
     * default, this is the root or default resource in your application,
     * but can be configured for each environment.
     *
     * Here, we report success as long as the app server is up, but skip
     * generating the whole page since this is a HEAD request only. You
     * can employ more sophisticated health checks in your application.
     *
     * @param model the spring model for the request
     */
    @RequestMapping(value="/home.do", method=RequestMethod.HEAD)
    public void doHealthCheck(HttpServletResponse response) {
        response.setContentLength(0);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    /**
     * The main request handler that builds out the home page for the journal
     * @param model the spring model for the request
     */
    @RequestMapping(value="/home.do", method={RequestMethod.GET, RequestMethod.POST})
    public void doHome (ModelMap model) {
        Journal journal = dao.getJournal();
        List<User> users = dao.getUsers();

        //Create placeholder objects for the various forms in the page
        if (model.get("entry")==null) {
            Entry entry = new Entry();
            model.addAttribute("entry",entry);
        }

        if (model.get("photo")==null) {
            Photo photo = new Photo();
            model.addAttribute("photo",photo);
        }

        if (model.get("comment")==null) {
            Comment comment = new Comment();
            model.addAttribute("comment",comment);
        }


        //If we have no journal it means this is a new journal, so bootstrap it
        if (journal == null) {
            runWizard(model);
            return;
        }
        else if (users.size()==0) {
            //If we have no users, bootstrap it
            runWizard(model);
            return;
        }


        model.addAttribute("journal",journal);
        Collection<Entry> entries = dao.getEntries(journal);

        loadPhotos(model, entries);
        loadComments(model, entries);


        model.addAttribute("entries",entries);
    }

    /**
     * If we have a login failure this request mapping flags the error to be shown
     * in the UI.
     * @param model the spring model for the request
     */
    @RequestMapping ("/loginFailure.do")
    public ModelAndView doLoginFailure (ModelMap map) {
        map.addAttribute("popupScreen","login_div");
        doHome(map);
        return new ModelAndView("home", map);
    }

    /**
     * Creates a map of photos tied to entry id's used to display photos in the UI
     * @param model the spring model map to bring in request values
     * @param entries collection of entries that photos are attached to
     */
    private void loadPhotos(ModelMap model, Collection<Entry> entries) {
        //Create a map of entries to photos
        Map<String,List<Photo>> photoMap = new LinkedHashMap<String,List<Photo>>();
        model.addAttribute("photoMap", photoMap);

        //SimpleDB doesn't have joins so querying these one at a time isn't less efficient
        for (Entry entryItem: entries) {
            List<Photo>photos = dao.getPhotos(entryItem);
            photoMap.put(entryItem.getId(),photos);
        }
    }

    /**
     * Creates a map of comments tied to entry id's used to display comments in the UI
     * @param model the spring model map to bring in request values
     * @param entries collection of entries that comments are attached to
     */
    private void loadComments(ModelMap model, Collection<Entry> entries) {
        //Create a map of entries to photos
        Map<String,List<Comment>> commentMap = new LinkedHashMap<String,List<Comment>>();
        model.addAttribute("commentMap", commentMap);

        //SimpleDB doesn't have joins so querying these one at a time isn't less efficient
        for (Entry entryItem: entries) {
            List<Comment>photos = dao.getComments(entryItem);
            commentMap.put(entryItem.getId(),photos);
        }
    }


    private void runWizard(ModelMap model) {
        //We need to launch the bootstrap interface
        model.addAttribute("bootstrap",true);

        if (model.get("user")==null) {
            List<User> users = dao.getUsers();
            if (users.size()>0) {
                //One user exists in the database which is all we need
                model.addAttribute("usercreated",true);

            }
            model.addAttribute("user",new User());
        }
        if (model.get("journal")==null) {
            model.addAttribute("journal",new Journal());
        }
        return;
    }

    @RequestMapping("/createAccount.do")
    public ModelAndView doCreateAccount (User user,  BindingResult result, ModelMap map,
      @RequestParam("password2") String password2) {

        //Verify user info submission
        if (user.getUsername().equals("")) {
            result.reject("username", "Username cannot be blank");
        }
        if (user.getPassword().equals("")) {
            result.reject("password", "Password cannot be blank");
        }
        if (!user.getPassword().equals(password2)) {
            result.reject("password", "Passwords do not match");
        }

        if (result.hasErrors()) {
            doHome(map);
            return new ModelAndView("home", map);
        }

        // check to make sure we don't have a user account already
        List<User> users = dao.getUsers();
        if (users.size() > 0) {
            result.reject("username", "The admin user already exists");
            return new ModelAndView("home", map);
        } else {
            dao.saveUser(user);
            map.addAttribute("usercreated", true);
        }

        doHome(map);
        return new ModelAndView("redirect:home.do");

    }



    @RequestMapping("/createJournal.do")
    public ModelAndView doCreateJournal (Journal journal,  BindingResult result,
      @RequestParam("preload") boolean preload, ModelMap map) {
        //If we're preloading the sample, just skip to the preload method
        if (preload) {
            preloadJournal();
        } else {
            if (journal.getTitle().length()==0) {
                result.reject("title","Title cannot be blank");
            }

            //check to make sure we don't already have a journal
            if (dao.getJournals().size()>0) {
                result.reject("title","Journal already exists.  Please reload page.");
            }

            if (!result.hasErrors()) {
                dao.saveJournal(journal);
            }
        }

        doHome(map);
        if (result.hasErrors()) {
            return new ModelAndView("home",map);
        }
        else {
            return new ModelAndView("redirect:home.do");
        }
    }

    private void preloadJournal() {
        Configuration config = Configuration.getInstance();
        String bucket = config.getProperty("bundleBucket");
        String path = config.getProperty("bundlePath");
        DataLoader loader = new DataLoader(bucket,path,dao);
        Thread thread = new Thread(loader);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            logger.log(Level.WARNING,e.getMessage(),e);
        }
    }

    @RequestMapping("/deleteEntry.do")
    @Secured("ROLE_ADMIN")
    public ModelAndView doDeleteEntry (@RequestParam("entryId") String entryId, ModelMap map) {
        Entry entry = dao.getEntry(entryId);
        dao.deleteEntry(entry);
        doHome(map);
        return new ModelAndView("redirect:home.do");
    }

    @RequestMapping("/deletePhoto.do")
    @Secured("ROLE_ADMIN")
    public ModelAndView doDeletePhoto (@RequestParam("photoId") String photoId, ModelMap map) {
        Photo photo = dao.getPhoto(photoId);
        if (photo == null) {
            //Photo shouldn't ever be null, but just in case we handle it
            doHome(map);
            return new ModelAndView("redirect:home.do");
        }

        S3PhotoUtil.deletePhotoFromS3(photo);
        dao.deletePhoto(photo);

        doHome(map);
        return new ModelAndView("redirect:home.do");

    }

    @RequestMapping("/deleteComment.do")
    @Secured("ROLE_ADMIN")
    public ModelAndView doDeleteComment (@RequestParam("commentId") String commentId, ModelMap map) {
        Comment comment = dao.getComment(commentId);
        if (comment == null) {
            //Photo shouldn't ever be null, but just in case we handle it
            doHome(map);
            return new ModelAndView("redirect:home.do");
        }

        dao.deleteComment(comment);
        doHome(map);
        return new ModelAndView("redirect:home.do");

    }

    @RequestMapping("/saveEntry.do")
    @Secured("ROLE_ADMIN")
    public ModelAndView doSaveEntry (Entry entry, BindingResult result, ModelMap map) {
        if (entry.getTitle().length()==0) {
            result.reject("title","You must enter a title for this entry");
        }

        if (result.hasErrors()) {
            doHome(map);
            map.addAttribute("popupScreen","entry_div");
            return new ModelAndView("home",map);
        }
        Journal journal = dao.getJournal();
        entry.setJournal(journal);

        //Make an initial save to get entry id populated
        dao.saveEntry(entry);

        TravelLogSNSManager sns = new TravelLogSNSManager();
        sns.createTopic(entry);

        //save with arn set
        dao.saveEntry(entry);

        doHome(map);
        return new ModelAndView("redirect:home.do");
    }

    @RequestMapping("/saveComment.do")
    public ModelAndView doSaveComment (Comment comment, BindingResult result,
      ModelMap map, HttpServletRequest request) {

        if (comment.getBody().length()==0) {
            result.reject("body","You did not enter a comment");
        }

        if (comment.getCommenter().getName().length()==0) {
            result.reject("commenter.name","You must enter a name");
        }
        if (result.hasErrors()) {
            doHome(map);
            map.addAttribute("popupScreen","comment_div");
            return new ModelAndView("home",map);
        }

        Entry entry = dao.getEntry(comment.getEntry().getId());
        comment.setEntry(entry);
        comment.setDate(Calendar.getInstance().getTime());
        dao.saveCommenter(comment.getCommenter());
        dao.saveComment(comment);



        TravelLogSNSManager sns = new TravelLogSNSManager();
        if (request.getParameter("emailComments")!=null) {
            sns.subscribe(entry, comment.getCommenter());
        }

        sns.publish(entry, comment);

        doHome(map);

        return new ModelAndView("redirect:home.do");
    }

    @RequestMapping("/uploadPhoto.do")
    @Secured("ROLE_ADMIN")
    public ModelAndView doUploadPhoto (Photo photo, BindingResult result, @RequestParam("file") MultipartFile file,
      @RequestParam("entryId") String entryId, ModelMap map) {
        try {
            Entry entry = dao.getEntry(entryId);
            photo.setEntry(entry);


            if (file.getSize()>0) {
                //We must save photo to ensure we have a photo ID for storing images on S3
                dao.savePhoto(photo);

                //A file has been uploaded so do a full storage routine
                photo = S3PhotoUtil.storePhoto(photo, file.getBytes());
                dao.savePhoto(photo);
            }
            else if (file.getSize()==0 && StringUtils.isEmpty(photo.getId())) {
                //No file uploaded for a new photo, so error out
                result.reject("file","You must specify a photo to upload");
                doHome(map);
                map.addAttribute("popupScreen","photo_div");
                return new ModelAndView("home",map);
            }
            else {
                //Updating existing photo

                //Need to grab the paths to the photos from the database
                //as these aren't changeable except by an upload
                Photo originalPhoto = dao.getPhoto(photo.getId());
                photo.setOriginalPath(originalPhoto.getOriginalPath());
                photo.setWebsizePath(originalPhoto.getWebsizePath());
                photo.setThumbnailPath(originalPhoto.getThumbnailPath());

                //No uploaded photo so just save the updated data
                dao.savePhoto(photo);
            }
        }
        catch (IOException e) {
            logger.log(Level.SEVERE,e.getMessage(),e);
            result.reject("file","There was an error uploading the photo");
            doHome(map);
            map.addAttribute("popupScreen","photo_div");
            return new ModelAndView("home",map);
        }

        doHome(map);
        return new ModelAndView("redirect:home.do");
    }

    @RequestMapping("/logout.do")
    public void doLogout (HttpServletResponse response) throws IOException {
        response.sendRedirect("home.do");
    }

    @RequestMapping("/backupRestore.do")
    @Secured("ROLE_ADMIN")
    public ModelAndView doBackupRestore (ModelMap map, @RequestParam("backupBucket") String bucketName,
        @RequestParam("backupPath") String storagePath, @RequestParam("backupRestoreFlag") String flag) {

        final Thread thread;
        if (flag.equals("backup")) {
            DataExtractor extractor = new DataExtractor(bucketName,storagePath,dao);
            thread = new Thread(extractor);
        }
        else {
            DataLoader loader = new DataLoader(bucketName, storagePath, dao);
            thread = new Thread(loader);
        }

        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        doHome(map);
        return new ModelAndView("redirect:home.do");
    }


    @Autowired
    public void setTravelLogDAO (TravelLogDAO dao) {
        this.dao = dao;
    }

    /**
     * Method establishes the transformation of incoming date strings into Date objects
     * @param binder the spring databinder object that we connect to the date editor
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        dateFormat.setLenient(true);
        binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
    }

}
