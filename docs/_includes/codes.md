{% assign lang = '' %}
{% assign full = 'English' %}
{% assign parent = '/' %}

{% if page.url contains 'ar.html' %}{% assign lang = 'ar' %}{% assign full = 'العربية' %}{% endif %}
{% if page.url contains 'de.html' %}{% assign lang = 'de' %}{% assign full = 'Deutsch' %}{% endif %}
{% if page.url contains 'es.html' %}{% assign lang = 'es' %}{% assign full = 'Español' %}{% endif %}
{% if page.url contains 'fr.html' %}{% assign lang = 'fr' %}{% assign full = 'Français' %}{% endif %}
{% if page.url contains 'it.html' %}{% assign lang = 'it' %}{% assign full = 'Italiano' %}{% endif %}
{% if page.url contains 'ru.html' %}{% assign lang = 'ru' %}{% assign full = 'Русский' %}{% endif %}
{% if page.url contains 'zh.html' %}{% assign lang = 'zh' %}{% assign full = '中文' %}{% endif %}
{% if page.url contains 'pt.html' %}{% assign lang = 'pt' %}{% assign full = 'Portugal' %}{% endif %}

{% assign mylast = page.dir | split: "/" | last | append: "/" %}
{% assign parent = page.dir | remove: mylast %}

