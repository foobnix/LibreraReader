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

<style class="cp-pen-styles">* {
    font-family: arial;
}



.languagepicker {
    background-color: #989494;
    display: inline-block;
    padding: 0;
    height: 25px;
    overflow: hidden;
    transition: all .3s ease;
    margin: 0 25px 10px 0;
    vertical-align: top;
    float: left;
}

.languagepicker:hover {
    /* don't forget the 1px border */
    height: 300px;
}


.languagepicker li {
    display: block;
    padding: 0px 20px;
    line-height: 25px;
    border-top: 1px solid #EEE;
}



.languagepicker a:first-child li {
    border: none;
    background: #18bc9c !important;
}

.languagepicker li img {
    margin-right: 5px;
}

.roundborders {
    border-radius: 5px;
}


.large:hover {
    /* 
    don't forget the 1px border!
    The first language is 40px heigh, 
    the others are 41px
    */
    height: 255px;
}</style>

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
	       
	        