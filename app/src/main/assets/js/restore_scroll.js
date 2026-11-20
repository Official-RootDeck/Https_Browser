// Restore scroll coordinates
(function(x, y) {
  try {
    window.scrollTo({ left: x, top: y, behavior: 'instant' });
  } catch (e) {
    window.scrollTo(x, y);
  }
})(%X%, %Y%);
