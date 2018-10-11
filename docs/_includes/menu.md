{% assign find = false %}
 
{% if page.url contains 'ru.html' %}
{% assign find = true %}
[Русский](/wiki/ru)

{% include ru.md %}
{% endif %}


{% if page.url contains 'fr.html' %}
{% assign find = true %}
[Français](/wiki/fr)

{% include fr.md %}
{% endif %}

{% if page.url contains 'de.html' %}
{% assign find = true %}
  
[Deutsch](/wiki/de)

{% include de.md %}
{% endif %}

{% if page.url contains 'it.html' %}
{% assign find = true %}
  
[Deutsch](/wiki/de)

{% include de.md %}
{% endif %}


{% if page.url contains 'es.html' %}
{% assign find = true %}
  
[Español](/wiki/es)

{% include es.md %}
{% endif %}

{% if page.url contains 'pt.html' %}
{% assign find = true %}
  
[Portugal](/wiki/pt)

{% include pt.md %}
{% endif %}


{% if page.url contains 'zh.html' %}
{% assign find = true %}
  
[中文](/wiki/zh)

{% include zh.md %}
{% endif %}

{% if page.url contains 'ar.html' %}
{% assign find = true %}
  
[العربية](/wiki/ar)

{% include ar.md %}
{% endif %}


{% if find == false %}
 
[English](/wiki)

{% include index.md %}
{% endif %}

[English](/wiki)<br/>
[Русский](/wiki/ru)<br/>
[Français](/wiki/fr)<br/>
[Deutsch](/wiki/de)<br/>
[Portugal](/wiki/pt)<br/>
[Español](/wiki/es)<br/>
[العربية](/wiki/ar)<br/>
[中文](/wiki/zh)<br/>
	       
	        