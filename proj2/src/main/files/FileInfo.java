package main.files;

import java.io.Serializable;
import java.util.*;

public class FileInfo implements Serializable {
  String fileID, fileName;
  int desiredRepDegree;

  public FileInfo(String fileID, String fileName, int desiredRepDegree){
      this.fileID = fileID;
      this.fileName = fileName;
      this.desiredRepDegree = desiredRepDegree;
  }

  public FileInfo(String fileID){
      this.fileID = fileID;
  }

  public String toString() {
      return fileID + " # " + fileName + " # " + desiredRepDegree;
  }

  public String getFileName(){ return this.fileName; }
  public String getFileID(){ return this.fileID; }
  public int getDesiredRepDegree(){ return this.desiredRepDegree; }

  public int hashCode(){
      return Objects.hash(this.fileID);
  }

  public boolean equals(Object obj){
      if(this==obj) return true;
      if(obj instanceof FileInfo){
          FileInfo fi = (FileInfo)obj;
          if(fileID.equals(fi.fileID))
              return true;
          return false;
      }
      return false;
  }

}
