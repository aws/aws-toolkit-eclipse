function editEntry(entryId) {
	// load entry info via ajax
	AjaxController.getEntry(entryId, handleEditEntry);

}

function editPhoto(photoId) {
	// load entry info via ajax
	AjaxController.getPhoto(photoId, handleEditPhoto);

	// Hide the lightbox divs
	divOverlay = document.getElementById("overlay");
	divOverlay.style.display = 'none';
	divLightbox = document.getElementById("lightbox");
	divLightbox.style.display = 'none';
}

function handleEditEntry(entry) {
	document.entryForm.id.value = entry.id;
	document.entryForm.title.value = entry.title;
	document.entryForm.destination.value = entry.destination;
	document.entryForm.date.value = entry.formattedDate;
	document.entryForm.entryText.value = entry.entryText;
	showEntry();
}

function handleEditPhoto(photo) {
	document.photoForm.id.value = photo.id;
	document.photoForm.entryId.value = photo.entry.id;
	document.photoForm.title.value = photo.title;
	document.photoForm.date.value = photo.formattedDate;
	document.photoForm.subject.value = photo.subject;
	document.photoForm.description.value = photo.description;
	showPhoto();
}

function deleteEntry(entryId) {
	// load entry info via ajax
	if (confirm("Are you sure you want to delete this entry?")) {
		document.location.href = "deleteEntry.do?entryId=" + entryId;
	}

}

function deletePhoto(photoId) {
	// load entry info via ajax
	if (confirm("Are you sure you want to delete this photo?")) {
		document.location.href = "deletePhoto.do?photoId=" + photoId;
	}

}

function newEntry() {
	document.entryForm.id.value = "";
	document.entryForm.title.value = "";
	document.entryForm.destination.value = "";
	document.entryForm.entryText.value = "";
	document.entryForm.date.value = getCurrentFormattedDate();
	showEntry();
}

function getCurrentFormattedDate() {
	currentDate = new Date();
	day = currentDate.getDate();
	month = currentDate.getMonth() + 1;
	year = currentDate.getFullYear();
	return month + "/" + day + "/" + year;
}

function showEntry() {
	openPopup("entry_div");
	
}

function manageJournal() {
	openPopup("manage_div");
}

function wait(waitElementId) {
	waitElement = document.getElementById(waitElementId);
	waitElement.style.display = 'inline';
}

function uploadPhoto(entryId) {
	document.photoForm.id.value = "";
	document.photoForm.entryId.value = entryId;
	document.photoForm.title.value = "";
	document.photoForm.date.value = getCurrentFormattedDate();
	document.photoForm.subject.value = "";
	document.photoForm.description.value = "";
	showPhoto();
}

function addComment(entryId) {
	document.commentForm["entry.id"].value = entryId;
	openPopup("comment_div");
}

function deleteComment(commentId) {
	if (confirm("Are you sure you want to delete this comment?")) {
		document.location.href = "deleteComment.do?commentId=" + commentId;
	}
}

function showPhoto() {
	openPopup("photo_div");
}

function openPopupWithDimensions(divId, width, height) {
	$("#" + divId).modal( {
		minWidth : width,
		maxWidth : width,
		minHeight : height,
		opacity : 70,
		overlayCss : {
			backgroundColor : "#000",
			borderColor : "#000"
		},
		onOpen : function(dialog) {
			dialog.overlay.fadeIn('fast', function() {
				dialog.data.hide();
				dialog.container.fadeIn('fast', function() {
					dialog.data.fadeIn('fast');
				});
			});
		}
	});
}

function openPopup(divId) {
	openPopupWithDimensions(divId, 600);
}

var imageCount = 9; // max images displayed in the image gallery
function shiftGallery(index, entry_id, frames, max) {

	indexField = document.getElementById(index);

	if (frames < 0 && indexField.value == 0) {
		return;
	}
	if (frames > 0 && (Number(indexField.value) + imageCount) == max) {
		return;
	}

	// Add the frame count from the index
	indexField.value = Number(indexField.value) + frames;

	for (x = 0; x < max; x++) {
		currentPhoto = document.getElementById("photo_" + entry_id + "_" + x);
		if (x < indexField.value) {
			currentPhoto.style.display = 'none';
		} else if (x > indexField.value + imageCount) {
			currentPhoto.style.display = 'none';
		} else {
			currentPhoto.style.display = '';
		}

	}

}

function toggleJournalType (preload) {

	journalForm = document.getElementById("journal_form_table");
	journalForm.style.display=preload?'none':'block';
		
}

function confirmBackupRestore() {
	if (!confirm("Are you sure?")) {
		return false;
	}
	return true;
}

