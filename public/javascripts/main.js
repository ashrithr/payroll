/*Custom Functions*/
$.fn.formAsJson = function(){
  var o = {};
  var a = this.serializeArray();
  $.each(a, function () {
    if (o[this.name] !== undefined) {
      if (!o[this.name].push) {
        o[this.name] = [o[this.name]];
      }
      o[this.name].push(this.value || '');
    } else {
      o[this.name] = this.value || '';
    }
  });
  return JSON.stringify(o)
};

$(document).ready(function() {
  // enable timeago feature on all elements whose class contains timeago
  $('.timeago').each(function(i, e) {
    var time = moment($(e).attr("date-value"), "YYYY-MM-DDTHH:mm:ss.SSSZZ");
    $(e).text(time.fromNow());
  });

  $('.duein').each(function(i, e) {
    $(e).text(moment.duration(moment().diff(moment($(e).attr("date-value"), "YYYY-MM-DDTHH:mm:ss.SSSZZ"))).humanize());
  });

  // enable datepicker plugin on all elements whose class contains datepicker
  $('.datepicker').datepicker({orientation: 'bottom'});

  // enable select2 plugin on all elements whose class contains select2
  $('.select2').select2({
    theme: "bootstrap",
    width: '100%'
  });

  // enable tooltips on all elements who has data-toggle attribute
  $('[data-toggle=tooltip]').tooltip();
});