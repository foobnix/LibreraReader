{% assign find = false %}
 
{% if page.url contains 'ru.html' %}
{% assign find = true %}

[Русский](/wiki/ru)
    
* [О программе](/wiki/about/ru)
* [Руковдоство](/wiki/manual/ru)
* [Частые вопросы](/wiki/faq/ru)
* [Истории](/wiki/stories/ru)
* [Что нового](/wiki/what-is-new/ru)
* [Скачать](/wiki/download/ru)

{% endif %}


{% if page.url contains 'fr.html' %}
{% assign find = true %}
  
[Français](/wiki/fr)

* [À propos du programme](/wiki/about/fr)
* [Manuel](/wiki/manual/fr)
* [Foire aux questions](/wiki/faq/fr)
* [Histoires](/wiki/stories/fr)
* [Quoi de neuf](/wiki/what-is-new/fr)
* [Télécharger](/wiki/download/fr)
 
{% endif %}


{% if page.url contains 'de.html' %}
{% assign find = true %}
  
[Deutsch](/wiki/de)

* [Über Librera](/wiki/about/de)
* [Guide](/wiki/manual/de)
* [FAQ](/wiki/faq/de)
* [Geschichten](/wiki/stories/de)
* [Was ist neu](/wiki/what-is-new/de)
* [Download](/wiki/download/de)
 
{% endif %}



{% if find == false %}
 
[English](/wiki)

* [About Librera](/wiki/about)
* [Guide](/wiki/manual)
* [FAQ](/wiki/faq)
* [Stories](/wiki/stories)
* [What is new](/wiki/what-is-new/)
* [Download](/wiki/download)

{% endif %}
   
[English](/wiki)   
[Русский](/wiki/ru)   
[Français](/wiki/fr)  
[Deutsch](/wiki/de)
	       
	        