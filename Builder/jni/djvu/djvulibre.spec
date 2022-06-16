%define release 1
%define version 3.5.28

Summary: DjVu viewers, encoders and utilities.
Name: djvulibre
Version: %{version}
Release: %{release}
License: GPL
Group: Applications/Publishing
Source: djvulibre-%{version}.tar.gz
BuildRoot: %{_tmppath}/%{name}-root
URL: http://djvu.sourceforge.net

# BuildRequires: qt-devel
# BuildRequires: qt-designer
# BuildRequires: libjpeg-devel
# BuildRequires: libtiff-devel
# BuildRequires: glibc-devel
# BuildRequires: gcc-c++

%description 

DjVu is a web-centric format and software platform for distributing documents
and images.  DjVu content downloads faster, displays and renders faster, looks
nicer on a screen, and consume less client resources than competing formats.
DjVu was originally developed at AT&T Labs-Research by Leon Bottou, Yann
LeCun, Patrick Haffner, and many others.  In March 2000, AT&T sold DjVu to
LizardTech Inc. who now distributes Windows/Mac plug-ins, and commercial
encoders (mostly on Windows)

In an effort to promote DjVu as a Web standard, the LizardTech management was
enlightened enough to release the reference implementation of DjVu under the
GNU GPL in October 2000.  DjVuLibre (which means free DjVu), is an enhanced
version of that code maintained by the original inventors of DjVu. It is
compatible with version 3.5 of the LizardTech DjVu software suite.

DjVulibre-3.5 contains:
- An up-to-date version of the C++ DjVu Reference Library.
- A full-fledged wavelet-based compressor for pictures. 
- A simple compressor for bitonal (black and white) scanned pages. 
- A compressor for palettized images (a la GIF/PNG). 
- A set of utilities to manipulate and assemble DjVu images and documents. 
- A set of decoders to convert DjVu to a number of other formats. 

%prep
%setup -q

%build
%configure
make depend
make

%install
rm -rf %{buildroot}
make DESTDIR=%{buildroot} install

# Quick fix to stop ldconfig from complaining
find %{buildroot}%{_libdir} -name "*.so*" -exec chmod 755 {} \;

# Quick cleanup of the docs
rm -rf doc/CVS 2>/dev/null || :

%clean
rm -rf %{buildroot}

%post 
# LIBS: Run ldconfig
/sbin/ldconfig
exit 0

%postun
# LIBS: Run ldconfig
/sbin/ldconfig
exit 0


%files
%defattr(-, root, root)
%doc README COPYRIGHT COPYING NEWS doc
%{_bindir}
%{_libdir}
%{_includedir}/libdjvu
%{_datadir}
%{_mandir}

%changelog
* Sun Feb 08 2015 Leon Bottou <leon@bottou.org> 3.5.27-1
- new release
