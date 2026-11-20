/* Built by MD.Nirob */
(function() {
    function getMetaContent(names) {
        for (var i = 0; i < names.length; i++) {
            var el = document.querySelector('meta[name="' + names[i] + '"], meta[property="' + names[i] + '"]');
            if (el && el.getAttribute('content')) {
                return el.getAttribute('content').trim();
            }
        }
        return '';
    }

    var title = getMetaContent(['og:title', 'twitter:title']) || document.title || '';
    var h1 = document.querySelector('h1');
    if (h1 && h1.innerText && h1.innerText.trim().length > 4) {
        title = h1.innerText.trim();
    }

    var byline = getMetaContent(['author', 'article:author', 'twitter:creator']) || '';
    if (!byline) {
        var authorEl = document.querySelector('[rel="author"], .author, .byline');
        if (authorEl) byline = authorEl.innerText.trim();
    }

    var siteName = getMetaContent(['og:site_name']) || window.location.hostname || '';
    var excerpt = getMetaContent(['description', 'og:description']) || '';

    var container = document.querySelector('article, main, [role="main"], .post-content, .article-body, .entry-content') || document.body;
    var paragraphs = [];
    if (container) {
        var nodes = container.querySelectorAll('h1, h2, h3, p, blockquote, li');
        for (var j = 0; j < nodes.length; j++) {
            var node = nodes[j];
            if (node.closest('nav, footer, header, aside, .ad, .ads, .comments, script, style')) continue;
            var txt = (node.innerText || '').replace(/\s+/g, ' ').trim();
            if (txt.length >= 25 || (node.tagName.match(/^H[1-3]$/) && txt.length >= 4)) {
                if (paragraphs.indexOf(txt) === -1) {
                    paragraphs.push(txt);
                }
            }
        }
    }

    if (paragraphs.length === 0 && document.body) {
        var raw = (document.body.innerText || '').split(/\n+/);
        for (var k = 0; k < raw.length; k++) {
            var line = raw[k].trim();
            if (line.length >= 25) paragraphs.push(line);
        }
    }

    var fullText = paragraphs.join('\n\n');
    var wordCount = fullText ? fullText.split(/\s+/).length : 0;
    var readingTimeMinutes = Math.max(1, Math.ceil(wordCount / 200));
    if (!excerpt && paragraphs.length > 0) {
        excerpt = paragraphs[0].substring(0, 180);
    }

    return JSON.stringify({
        title: title,
        byline: byline,
        siteName: siteName,
        excerpt: excerpt,
        paragraphs: paragraphs.slice(0, 250),
        textContent: fullText.substring(0, 100000),
        readingTimeMinutes: readingTimeMinutes
    });
})();
