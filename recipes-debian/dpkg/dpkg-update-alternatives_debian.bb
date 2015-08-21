require dpkg.inc

PR = "${INC_PR}.0"

DESCRIPTION = "Dpkg version of update-alternatives"

# Overwrite DEPENDS of dpkg.inc
DEPENDS = ""
PROVIDES += "virtual/update-alternatives"

SRC_URI += "\
	file://implement-offline-mode.patch \
	file://fix_update_alternatives_dir.patch \
	file://fix_offline_dir.patch \
"

# Unnecessary for update-alternatives
EXTRA_OECONF += " \
	--without-zlib \
	--without-bz2 \
"

# Only compile materials for update-alternatives
do_compile() {
	cd ${S}/lib && oe_runmake
	cd ${S}/utils && oe_runmake
}
  
do_install() {
	install -d ${D}${sbindir} 
	install -d ${D}${sysconfdir}/alternatives 
	install -d ${D}${localstatedir}/lib/dpkg/alternatives
	install -m 0755 ${S}/utils/update-alternatives ${D}${sbindir}
	exit 0
}

PACKAGES = "${PN} ${PN}-dbg"
FILES_${PN} = " \
	${sbindir}/update-alternatives \
	${localstatedir}/lib/dpkg/alternatives \
	${sysconfdir}/alternatives \
"
FILES_${PN}-dbg = " \
	${prefix}/src \
	${sbindir}/.debug \
"
