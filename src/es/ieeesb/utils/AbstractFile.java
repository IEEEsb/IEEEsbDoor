package es.ieeesb.utils;

public class AbstractFile
{
	public String filename;
	public String content;
	public Usuario owner;
	public String profile;

	public AbstractFile(String filename, String content, Usuario owner, String profile)
	{
		this.filename = filename;
		this.content = content;
		this.owner = owner;
		this.profile = profile;
	}
}
