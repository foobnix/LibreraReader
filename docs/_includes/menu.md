{% assign find = false %}
 
{% if page.url contains 'ru.html' %}
{% assign find = true %}
{% include ru.md %}
{% endif %}


{% if page.url contains 'fr.html' %}
{% assign find = true %}
{% include fr.md %}
{% endif %}

{% if page.url contains 'de.html' %}
{% assign find = true %}
{% include de.md %}
{% endif %}

{% if page.url contains 'it.html' %}
{% assign find = true %}
{% include it.md %}
{% endif %}


{% if page.url contains 'es.html' %}
{% assign find = true %}
{% include es.md %}
{% endif %}

{% if page.url contains 'pt.html' %}
{% assign find = true %}
{% include pt.md %}
{% endif %}


{% if page.url contains 'zh.html' %}
{% assign find = true %}
{% include zh.md %}
{% endif %}

{% if page.url contains 'ar.html' %}
{% assign find = true %}
{% include ar.md %}
{% endif %}


{% if find == false %}
{% include index.md %}
{% endif %}

[English](/wiki)<br/>
[Русский](/wiki/ru)<br/>
[Français](/wiki/fr)<br/>
[Deutsch](/wiki/de)<br/>
[Italiano](/wiki/it)<br/>
[Portugal](/wiki/pt)<br/>
[Español](/wiki/es)<br/>
[العربية](/wiki/ar)<br/>
[中文](/wiki/zh)<br/>
	       
	        