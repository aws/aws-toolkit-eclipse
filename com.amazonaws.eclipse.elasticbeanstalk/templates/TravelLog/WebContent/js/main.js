jQuery(function($) {
    $('a[href="#TODO"]').click(function() {
        alert('Nothing to see here, boss.');
    });
    
    $('#entries .entry .expando').click(function() {
        $(this)
            .closest('.entry')
            .toggleClass('open')
            .find('.body')
            .slideToggle()
            .end()
    });

});