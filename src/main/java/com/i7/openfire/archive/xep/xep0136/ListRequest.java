package com.i7.openfire.archive.xep.xep0136;

import java.util.Date;

import org.dom4j.Element;
import org.dom4j.QName;

import com.i7.openfire.archive.util.XmppDateUtil;
import com.i7.openfire.archive.xep.xep0059.XmppResultSet;

/**
 * A request to retrieve a list of collections.
 */
public class ListRequest {
	
	private Date end;
	private Date start;
	private String with;
	private XmppResultSet resultSet;

	public ListRequest(Element listElement) {
		if (listElement.attribute("with") != null) {
			this.with = listElement.attributeValue("with");
		}
		if (listElement.attribute("start") != null) {
			this.start = XmppDateUtil.parseDate(listElement.attributeValue("start"));
		}
		if (listElement.attribute("end") != null) {
			this.end = XmppDateUtil.parseDate(listElement.attributeValue("end"));
		}

		Element setElement = listElement.element(QName.get("set", XmppResultSet.NAMESPACE));
		if (setElement != null) {
			resultSet = new XmppResultSet(setElement);
		}
	}

	public String getWith() {
		return with;
	}

	public Date getStart() {
		return start;
	}

	public Date getEnd() {
		return end;
	}

	public XmppResultSet getResultSet() {
		return resultSet;
	}
}
