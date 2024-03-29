/*
 * Minio Java Library for Amazon S3 Compatible Cloud Storage, (C) 2015 Minio, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hk.edu.polyu.intercloud.minio.messages;

import com.google.api.client.xml.Xml;
import com.google.api.client.xml.GenericXml;
import com.google.api.client.xml.XmlNamespaceDictionary;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.Reader;
import java.io.IOException;


public abstract class XmlEntity extends GenericXml {
  private XmlPullParser xmlPullParser;
  private XmlNamespaceDictionary defaultNamespaceDictionary;


  /**
   * constructor.
   */
  public XmlEntity() throws XmlPullParserException {
    super.namespaceDictionary = new XmlNamespaceDictionary();
    super.namespaceDictionary.set("s3", "http://s3.amazonaws.com/doc/2006-03-01");
    super.namespaceDictionary.set("", "");

    this.xmlPullParser = Xml.createParser();
    this.defaultNamespaceDictionary = new XmlNamespaceDictionary();
  }


  public XmlEntity(Reader reader) throws IOException, XmlPullParserException {
    this();
    this.parseXml(reader);
  }


  public void parseXml(Reader reader) throws IOException, XmlPullParserException {
    this.xmlPullParser.setInput(reader);
    Xml.parseElement(this.xmlPullParser, this, this.defaultNamespaceDictionary, null);
  }


  protected void parseXml(Reader reader, XmlNamespaceDictionary namespaceDictionary)
    throws IOException, XmlPullParserException {
    this.xmlPullParser.setInput(reader);
    Xml.parseElement(this.xmlPullParser, this, namespaceDictionary, null);
  }
}
