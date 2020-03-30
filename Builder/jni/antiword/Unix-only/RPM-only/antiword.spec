
%define version	0.33
%define release	1
%define name	antiword

Summary: an application to display Microsoft(R) Word files.
Name: %{name}
Version: %{version}
Release: %{release}
License: GPL
Group: Applications/Text
Source: http://www.winfield.demon.nl/linux/%{name}-%{version}.tar.gz
URL: http://www.winfield.demon.nl/index.html
BuildRoot: /var/tmp/%{name}-%{version}
Packager: marco antonio cabazal <nightshiphter@yahoo.com>

%description
Antiword is a free MS Word reader for Linux and RISC OS. There are ports to 
BeOS, OS/2, Mac OS X, Amiga, VMS, NetWare and DOS. Antiword converts the 
binary files from Word 2, 6, 7, 97, 2000 and 2002 to plain text and to 
PostScript TM.
.

%prep
# nothing to be done here

%build
make all

%install
rm -rf $RPM_BUILD_ROOT
install -d 555 $RPM_BUILD_ROOT/%{_prefix}/bin
install -d 555 $RPM_BUILD_ROOT/%{_prefix}/share/antiword
install -d 555 $RPM_BUILD_ROOT/%{_prefix}/share/man/man1
install -m 555 ./antiword $RPM_BUILD_ROOT%{_prefix}/bin/antiword
install -m 555 ./kantiword $RPM_BUILD_ROOT%{_prefix}/bin/kantiword
install -m 444 ./Resources/* $RPM_BUILD_ROOT%{_prefix}/share/antiword
install -m 444 ./Docs/antiword.1 $RPM_BUILD_ROOT/%{_prefix}/share/man/man1/antiword.1

%clean
rm -rf $RPM_BUILD_ROOT

%files
%defattr(-,root,root)
%doc Docs/*
%{_prefix}/bin/*
%{_prefix}/share/antiword/*
%{_prefix}/share/man/man1/*

