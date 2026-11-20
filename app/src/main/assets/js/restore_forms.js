// Restore form inputs from serialized map
(function(formValues) {
  try {
    if (!formValues) return;
    const inputs = document.querySelectorAll('input, textarea, select');
    inputs.forEach(el => {
      const key = el.name || el.id;
      if (key && formValues.hasOwnProperty(key)) {
        if (el.type === 'checkbox' || el.type === 'radio') {
          el.checked = (formValues[key] === 'true');
        } else {
          el.value = formValues[key];
        }
      }
    });
  } catch (e) {
    console.error('Error restoring form values', e);
  }
})(%FORM_JSON%);
