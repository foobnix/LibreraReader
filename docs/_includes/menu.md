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



<br/>
<ul class="languagepicker roundborders large">
	<a href="#"><li>language</li></a>
    <a href="index"><li>English</li></a>
    <a href="ru"><li>Русский</li></a>
	<a href="fr"><li>Français</li></a>
    <a href="de"><li>Deutsch</li></a>
    <a href="it"><li>Italiano</li></a>
    <a href="pt"><li>Portugal</li></a>
    <a href="es"><li>Español</li></a>
    <a href="ar"><li>العربية</li></a>
    <a href="zh"><li>中文</li></a>
</ul>
<br/>
	       
	        